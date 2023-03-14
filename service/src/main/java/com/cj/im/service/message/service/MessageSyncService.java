package com.cj.im.service.message.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import com.cj.codec.pack.message.MessageReadedPack;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.command.Command;
import com.cj.im.common.enums.command.GroupEventCommand;
import com.cj.im.common.enums.command.MessageCommand;
import com.cj.im.common.model.SyncReq;
import com.cj.im.common.model.SyncResp;
import com.cj.im.common.model.message.MessageReadedContent;
import com.cj.im.common.model.message.MessageReciveAckContent;
import com.cj.im.common.model.message.OfflineMessageContent;
import com.cj.im.service.conversation.service.ConversationService;
import com.cj.im.service.utils.MessageProduce;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class MessageSyncService {

    @Autowired
    MessageProduce messageProducer;
//
    @Autowired
    ConversationService conversationService;

    @Autowired
    StringRedisTemplate redisTemplate;

    public void receiveMark(MessageReciveAckContent messageReciveAckContent){
        messageProducer.sendToUser(messageReciveAckContent.getToId(),
                MessageCommand.MSG_RECIVE_ACK,messageReciveAckContent,messageReciveAckContent.getAppId());
    }

    /**
     * @description: 消息已读。更新会话的seq，通知在线的同步端发送指定command ，发送已读回执通知对方（消息发起方）我已读
     * 1053 是我们同步端发给我们自己的
     * 1054 是其他人发给我们的，说这条消息我已经读了
     * @param
     * @return void
     * @author cj
     */
    public void readMark(MessageReadedContent messageContent) {

        conversationService.messageMarkRead(messageContent);
        //发给自己其他端
        syncToSender(messageContent);
        //发送给对方
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageContent,messageReadedPack);
        messageProducer.sendToUser(messageContent.getToId(),
                MessageCommand.MSG_READED_RECEIPT,messageReadedPack,messageContent.getAppId());

//        conversationService.messageMarkRead(messageContent);
//        MessageReadedPack messageReadedPack = new MessageReadedPack();
//        BeanUtils.copyProperties(messageContent,messageReadedPack);
//        syncToSender(messageReadedPack,messageContent,MessageCommand.MSG_READED_NOTIFY);
//        //发送给对方
//        messageProducer.sendToUser(messageContent.getToId(),
//                MessageCommand.MSG_READED_RECEIPT,messageReadedPack,messageContent.getAppId());
    }

    private void syncToSender(MessageReadedContent messageReadedContent){
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtil.copyProperties(messageReadedContent,messageReadedPack);

        //发送给自己的其他端
        messageProducer.sendToUserExceptClient(messageReadedContent.getToId(),MessageCommand.MSG_READED_NOTIFY,
                messageReadedContent,messageReadedContent);
    }

    private void syncToSender(MessageReadedPack pack, MessageReadedContent messageReadedContent, Command command){
//        MessageReadedPack messageReadedPack = new MessageReadedPack();
//        BeanUtils.copyProperties(messageReadedContent,messageReadedPack);
//        //发送给自己的其他端
//        messageProducer.sendToUserExceptClient(pack.getFromId(),
//                command,pack,
//                messageReadedContent);
    }

//    public void groupReadMark(MessageReadedContent messageReaded) {
//        conversationService.messageMarkRead(messageReaded);
//        MessageReadedPack messageReadedPack = new MessageReadedPack();
//        BeanUtils.copyProperties(messageReaded,messageReadedPack);
//        syncToSender(messageReadedPack,messageReaded, GroupEventCommand.MSG_GROUP_READED_NOTIFY
//        );
//        if(!messageReaded.getFromId().equals(messageReaded.getToId())){
//            messageProducer.sendToUser(messageReadedPack.getToId(),GroupEventCommand.MSG_GROUP_READED_RECEIPT
//                    ,messageReaded,messageReaded.getAppId());
//        }
//    }

    /**
     * 同步离线消息
     * @param req
     * @return
     */
    public ResponseVO syncOfflineMessage(SyncReq req) {

        SyncResp<OfflineMessageContent> resp = new SyncResp<>();

        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + req.getOperater();
        //获取最大的seq
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);
        //不为空
        if(!CollectionUtils.isEmpty(set)){
            List list = new ArrayList(set);
            //返回一个socre value对象数据结构
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = o.getScore().longValue();
        }

        //组装返回的数据
        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSequence(maxSeq);


        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(key,
                req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        //判断是否查询完了
        if(!CollectionUtils.isEmpty(respList)){
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq <= offlineMessageContent.getMessageKey());
        }

        return ResponseVO.successResponse(resp);
    }
}
