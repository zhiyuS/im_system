package com.cj.im.service.group.service;


import com.cj.codec.pack.message.ChatMessageAck;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.command.GroupEventCommand;
import com.cj.im.common.model.ClientInfo;
import com.cj.im.common.model.message.GroupChatMessageContent;
import com.cj.im.common.model.message.MessageContent;
import com.cj.im.common.model.message.OfflineMessageContent;
import com.cj.im.service.group.model.req.GroupMemberDto;
import com.cj.im.service.group.model.req.SendGroupMessageReq;
import com.cj.im.service.message.model.resp.SendMessageResp;
import com.cj.im.service.message.service.CheckSendMessageService;
import com.cj.im.service.message.service.MessageStoreService;
import com.cj.im.service.seq.RedisSeq;
import com.cj.im.service.utils.MessageProduce;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
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
public class GroupMessageService {



    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProduce messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    @Autowired
    MessageStoreService messageStoreService;
    @Autowired
    RedisSeq redisSeq;

    private final ThreadPoolExecutor threadPoolExecutor;

    {
        AtomicInteger num = new AtomicInteger();
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                //设置为守护线程
                thread.setDaemon(true);
                thread.setName("message-group-thread-" + num.getAndIncrement());
                return thread;
            }
        });
    }

    public void process(GroupChatMessageContent messageContent){
        String fromId = messageContent.getFromId();
        String groupId = messageContent.getGroupId();
        Integer appId = messageContent.getAppId();

        //判断是否在缓存中
        GroupChatMessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(),
                messageContent.getMessageId(), GroupChatMessageContent.class);
        if(messageFromMessageIdCache != null){
            threadPoolExecutor.execute(() ->{
                //1.回ack成功给自己
                ack(messageContent,ResponseVO.successResponse());
                //2.发消息给同步在线端
                syncToSender(messageContent,messageContent);
                //3.发消息给对方在线端
                dispatchMessage(messageContent);
            });
        }
        long seq = redisSeq.doGetSeq(messageContent.getAppId() + ":" + Constants.SeqConstants.GroupMessage
                + messageContent.getGroupId());
        messageContent.setMessageSequence(seq);

        threadPoolExecutor.execute(() ->{
            //持久化到数据库
            messageStoreService.storeGroupMessage(messageContent);

            List<String> groupMemberId = imGroupMemberService.getGroupMemberId(messageContent.getGroupId(),
                    messageContent.getAppId());
            messageContent.setMemberId(groupMemberId);

            //存储离线消息
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContent,offlineMessageContent);
            offlineMessageContent.setToId(messageContent.getGroupId());
            messageStoreService.storeGroupOfflineMessage(offlineMessageContent,groupMemberId);

            //1.回ack成功给自己
            ack(messageContent,ResponseVO.successResponse());
            //2.发消息给同步在线端
            syncToSender(messageContent,messageContent);
            //3.发消息给对方在线端
            dispatchMessage(messageContent);

            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(),
                    messageContent.getMessageId(),messageContent);
        });

    }

    /**
     * 发消息给群成员
     * @param messageContent
     */
    private void dispatchMessage(GroupChatMessageContent messageContent){

        List<String> groupMemberId = imGroupMemberService.getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId());

        for (String memberId : groupMemberId) {
            if(!memberId.equals(messageContent.getFromId())){
                messageProducer.sendToUser(memberId,
                        GroupEventCommand.MSG_GROUP,
                        messageContent,messageContent.getAppId());
            }
        }
    }

    private void ack(MessageContent messageContent, ResponseVO responseVO){

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        //發消息
        messageProducer.sendToUser(messageContent.getFromId(),
                GroupEventCommand.GROUP_MSG_ACK,
                responseVO,messageContent
        );
    }

    private void syncToSender(GroupChatMessageContent messageContent, ClientInfo clientInfo){
        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                GroupEventCommand.MSG_GROUP,messageContent,messageContent);
    }

    private ResponseVO imServerPermissionCheck(String fromId, String toId,Integer appId){
        ResponseVO responseVO = checkSendMessageService
                .checkGroupMessage(fromId, toId,appId);
        return responseVO;
    }

    public SendMessageResp send(SendGroupMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        BeanUtils.copyProperties(req,message);

        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        //2.发消息给同步在线端
        syncToSender(message,message);
        //3.发消息给对方在线端
        dispatchMessage(message);

        return sendMessageResp;

    }


}
