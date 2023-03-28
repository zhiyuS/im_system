package com.cj.im.service.message.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;


import com.cj.im.common.config.AppConfig;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ConversationTypeEnum;
import com.cj.im.common.enums.DelFlagEnum;
import com.cj.im.common.model.message.*;
import com.cj.im.service.group.dao.ImGroupMessageHistoryEntity;
import com.cj.im.service.group.dao.mapper.ImGroupMemberMapper;
import com.cj.im.service.group.dao.mapper.ImGroupMessageHistoryMapper;
import com.cj.im.service.message.dao.ImMessageBodyEntity;
import com.cj.im.service.message.dao.ImMessageHistoryEntity;
import com.cj.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.cj.im.service.message.dao.mapper.ImMessageHistoryMapper;
import com.cj.im.service.utils.SnowflakeIdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class MessageStoreService {

    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

//    @Autowired
//    ConversationService conversationService;

    @Autowired
    AppConfig appConfig;

    /**
     * 一对一发送消息
     * 拆分
     * @param messageContent
     */
    @Transactional
    public void storeP2PMessage(MessageContent messageContent){
        //messageContent 转化成 messageBody
        ImMessageBodyEntity imMessageBodyEntity = extractMessageBody(messageContent,1);
        //插入messageBody
        imMessageBodyMapper.insert(imMessageBodyEntity);
        //转化成MessageHistory
        List<ImMessageHistoryEntity> imMessageHistoryEntities = extractToP2PMessageHistory(messageContent, imMessageBodyEntity);
        //批量插入
        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
        messageContent.setMessageKey(imMessageBodyEntity.getMessageKey());

        //使用RabbitMQ持久化
//        ImMessageBody imMessageBodyEntity = extractMessageBody(messageContent);
//        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
//        dto.setMessageContent(messageContent);
//        dto.setMessageBody(imMessageBodyEntity);
//        messageContent.setMessageKey(imMessageBodyEntity.getMessageKey());
//        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage,"",
//                JSONObject.toJSONString(dto));
    }


    /**
     * 生成插入的数据实体
     * @param messageContent
     * @param notUseMq
     * @return
     */
    public ImMessageBodyEntity extractMessageBody(MessageContent messageContent,int notUseMq){
        ImMessageBodyEntity messageBody = new ImMessageBodyEntity();
        messageBody.setAppId(messageContent.getAppId());
        //雪花算法生成messageKey
        messageBody.setMessageKey(snowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        messageBody.setSecurityKey("");
        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());

        return messageBody;
    }
    public ImMessageBody extractMessageBody(MessageContent messageContent){
        ImMessageBody messageBody = new ImMessageBody();
        messageBody.setAppId(messageContent.getAppId());
        messageBody.setMessageKey(snowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        messageBody.setSecurityKey("");
        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());

        return messageBody;
    }

    public List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent,
                                                                   ImMessageBodyEntity imMessageBodyEntity){
        List<ImMessageHistoryEntity> list = new ArrayList<>();
        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setSequence(messageContent.getMessageSequence());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setSequence(messageContent.getMessageSequence());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }

    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent){
        ImMessageBodyEntity imMessageBody = extractMessageBody(messageContent,1);
        imMessageBodyMapper.insert(imMessageBody);
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity = extractToGroupMessageHistory(messageContent, imMessageBody);

        imGroupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
        messageContent.setMessageKey(imMessageBody.getMessageKey());

        //采用RabbitMQ异步持久化
//        DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
//        dto.setMessageBody(imMessageBody);
//
//        dto.setGroupChatMessageContent(messageContent);
//        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage,
//                "",
//                JSONObject.toJSONString(dto));

        messageContent.setMessageKey(imMessageBody.getMessageKey());
    }

    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent
                                                                     messageContent , ImMessageBodyEntity messageBodyEntity){
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,result);
        result.setGroupId(messageContent.getGroupId());
        result.setMessageKey(messageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
    }

    public void setMessageFromMessageIdCache(Integer appId,String messageId,Object messageContent){
        //appid : cache : messageId
        String key =appId + ":" + Constants.RedisConstants.cacheMessage + ":" + messageId;
        stringRedisTemplate.opsForValue().set(key,JSONObject.toJSONString(messageContent),300, TimeUnit.SECONDS);
    }

    public <T> T getMessageFromMessageIdCache(Integer appId,
                                              String messageId,Class<T> clazz){
        //appid : cache : messageId
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":" + messageId;
        String msg = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(msg)){
            return null;
        }
        return JSONObject.parseObject(msg, clazz);
    }

    /**
     * @description: 存储单人离线消息
     * @param
     * @return void
     * @author lld 
     */
    public void storeOfflineMessage(OfflineMessageContent offlineMessage){

        // 找到fromId的队列
        String fromKey = offlineMessage.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + offlineMessage.getFromId();
        // 找到toId的队列
        String toKey = offlineMessage.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + offlineMessage.getToId();

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        //判断 队列中的数据是否超过设定值
        if(operations.zCard(fromKey) > appConfig.getOfflineMessageCount()){
            operations.removeRange(fromKey,0,0);
        }
//

        // 插入 数据 根据messageKey 作为分值
        operations.add(fromKey,JSONObject.toJSONString(offlineMessage),
                offlineMessage.getMessageKey());

        //判断 队列中的数据是否超过设定值
        if(operations.zCard(toKey) > appConfig.getOfflineMessageCount()){
            operations.removeRange(toKey,0,0);
        }

//        offlineMessage.setConversationId(conversationService.convertConversationId(
//                ConversationTypeEnum.P2P.getCode(),offlineMessage.getToId(),offlineMessage.getFromId()
//        ));
        // 插入 数据 根据messageKey 作为分值
        operations.add(toKey,JSONObject.toJSONString(offlineMessage),
                offlineMessage.getMessageKey());

    }


    /**
     * @description: 存储单人离线消息
     * @param
     * @return void
     * @author lld
     */
    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessage
    ,List<String> memberIds){

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        //判断 队列中的数据是否超过设定值
        offlineMessage.setConversationType(ConversationTypeEnum.GROUP.getCode());

        for (String memberId : memberIds) {
            // 找到toId的队列
            String toKey = offlineMessage.getAppId() + ":" +
                    Constants.RedisConstants.OfflineMessage + ":" +
                    memberId;
//            offlineMessage.setConversationId(conversationService.convertConversationId(
//                    ConversationTypeEnum.GROUP.getCode(),memberId,offlineMessage.getToId()
//            ));
            if(operations.zCard(toKey) > appConfig.getOfflineMessageCount()){
                operations.removeRange(toKey,0,0);
            }
            // 插入 数据 根据messageKey 作为分值
            operations.add(toKey,JSONObject.toJSONString(offlineMessage),
                    offlineMessage.getMessageKey());
        }


    }

}
