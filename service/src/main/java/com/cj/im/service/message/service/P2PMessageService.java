package com.cj.im.service.message.service;


import cn.hutool.core.util.ObjectUtil;
import com.cj.codec.pack.message.ChatMessageAck;
import com.cj.codec.pack.message.MessageReciveServerAckPack;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.*;
import com.cj.im.common.enums.command.MessageCommand;
import com.cj.im.common.model.ClientInfo;
import com.cj.im.common.model.message.MessageContent;
import com.cj.im.common.model.message.OfflineMessageContent;
import com.cj.im.service.group.dao.ImGroupEntity;
import com.cj.im.service.group.model.resp.GetRoleInGroupResp;
import com.cj.im.service.group.service.ImGroupMemberService;
import com.cj.im.service.group.service.ImGroupService;
import com.cj.im.service.message.model.req.SendMessageReq;
import com.cj.im.service.message.model.resp.SendMessageResp;
import com.cj.im.service.seq.RedisSeq;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import com.cj.im.service.utils.ConversationIdGenerate;
import com.cj.im.service.utils.MessageProduce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class P2PMessageService {

    private static Logger logger = LoggerFactory.getLogger(P2PMessageService.class);

    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProduce messageProducer;
    @Autowired
    UserService userService;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    ImGroupMemberService imGroupMemberService;
    @Autowired
    MessageStoreService messageStoreService;
    @Autowired
    RedisSeq redisSeq;


    private final ThreadPoolExecutor threadPoolExecutor;

    {
        final AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("message-process-thread-" + num.getAndIncrement());
                return thread;
            }
        });
    }

    //离线
    //存储介质
    //1.mysql
    //2.redis
    //怎么存？
    //list


    //历史消息

    //发送方客户端时间
    //messageKey
    //redis 1 2 3
    public void process(MessageContent messageContent){

        //效验好友是否被禁言
        //发送方和接收方是否是好友
        String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();
        //TODO 用messageId从缓存中获取消息
        MessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(), MessageContent.class);

        //缓存命中直接返回
        if(ObjectUtil.isNotNull(messageFromMessageIdCache)){
            //回ack
            ack(messageFromMessageIdCache,ResponseVO.successResponse());
            //同步消息
            syncToSender(messageFromMessageIdCache,null);
            //发送消息给对方
            List<ClientInfo> clientInfos = dispatchMessage(messageFromMessageIdCache);
            //对方无在线的设备，服务端发送ack
            if (clientInfos.isEmpty()){
                reciverAck(messageFromMessageIdCache);
            }
            return;

        }
        //设置seq
        long seq = redisSeq.doGetSeq(messageContent.getAppId() + ":"
                + Constants.SeqConstants.Message+ ":" + ConversationIdGenerate.generateP2PId(
                messageContent.getFromId(),messageContent.getToId()
        ));
        messageContent.setMessageSequence(seq);

        ResponseVO responseVO = imServerPermissionCheck(fromId, toId, appId);
        if(responseVO.isOk()){

            threadPoolExecutor.execute(() -> {

                //插入离线消息并且设置messageKey
                messageStoreService.storeP2PMessage(messageContent);

                //插入离线消息
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                BeanUtils.copyProperties(messageContent,offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());

                messageStoreService.storeOfflineMessage(offlineMessageContent);

                //回ack
                ack(messageContent,responseVO);
                //同步消息
                syncToSender(messageContent,null);
                //发送消息给对方
                List<ClientInfo> clientInfos = dispatchMessage(messageContent);
                //TODO 将messageId存到缓存中
                messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(),messageContent.getMessageId(),messageContent);
                //对方无在线的设备，服务端发送ack
                if (clientInfos.isEmpty()){
                    reciverAck(messageContent);
                }
            });

        }else{
            ack(messageContent,responseVO);
        }


    }

    /**
     * 发消息给对面
     * @param messageContent
     * @return
     */
    private List<ClientInfo> dispatchMessage(MessageContent messageContent){
        List<ClientInfo> clientInfos = messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_P2P,
                messageContent, messageContent.getAppId());

        return clientInfos;
    }

    /**
     * 回ack给客户端
     * @param messageContent
     * @param responseVO
     */
    private void ack(MessageContent messageContent,ResponseVO responseVO){
        logger.info("msg ack,msgId={},checkResut{}",messageContent.getMessageId(),responseVO.getCode());

        ChatMessageAck chatMessageAck = new
                ChatMessageAck(messageContent.getMessageId(),messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);
        //發消息
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK,
                responseVO,messageContent
                );
    }

    /**
     * 服务器发送ack
     * @param messageContent
     */
    public void reciverAck(MessageContent messageContent){
        MessageReciveServerAckPack pack = new MessageReciveServerAckPack();
        pack.setFromId(messageContent.getToId());
        pack.setToId(messageContent.getFromId());
        pack.setMessageKey(messageContent.getMessageKey());
        pack.setMessageSequence(messageContent.getMessageSequence());
        pack.setServerSend(true);
        messageProducer.sendToUser(messageContent.getFromId(),MessageCommand.MSG_RECIVE_ACK,
                pack,new ClientInfo(messageContent.getAppId(),messageContent.getClientType()
                ,messageContent.getImei()));
    }

    /**
     * 同步给自己的其他端
     * @param messageContent
     * @param clientInfo
     */
    private void syncToSender(MessageContent messageContent, ClientInfo clientInfo){
            messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                    MessageCommand.MSG_P2P,messageContent,messageContent);
    }


    /**
     * 前置效验封装
     * @param fromId
     * @param toId
     * @param appId
     * @return
     */
    public ResponseVO imServerPermissionCheck(String fromId,String toId,
                                               Integer appId){
        ResponseVO responseVO = checkSendMessageService.checkSenderForvidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }

    /**
     * 效验群
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO checkGroupMessage(String fromId,String groupId,Integer appId){

        ResponseVO responseVO = checkSenderForvidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }

        //判断群逻辑
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(groupId, appId);
        if(!group.isOk()){
            return group;
        }

        //判断群成员是否在群内
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if(!roleInGroupOne.isOk()){
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();

        //判断群是否被禁言
        //如果禁言 只有裙管理和群主可以发言
        ImGroupEntity groupData = group.getData();
        if(groupData.getMute() == GroupMuteTypeEnum.MUTE.getCode()
                && (data.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() ||
                data.getRole() == GroupMemberRoleEnum.OWNER.getCode()  )){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }

        //判断群存活时间是否超时
        if(data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }

        return ResponseVO.successResponse();
    }

    /**
     * 前置效验群
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO checkSenderForvidAndMute(String fromId,Integer appId){

        ResponseVO<ImUserDataEntity> singleUserInfo
                = userService.getSingleUserInfo(fromId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        ImUserDataEntity user = singleUserInfo.getData();
        //是否被禁用与禁言
        if(user.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        }else if (user.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    public SendMessageResp send(SendMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        BeanUtils.copyProperties(req,message);
        //插入数据
        messageStoreService.storeP2PMessage(message);
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());

        //2.发消息给同步在线端
        syncToSender(message,message);
        //3.发消息给对方在线端
        dispatchMessage(message);
        return sendMessageResp;
    }

}
