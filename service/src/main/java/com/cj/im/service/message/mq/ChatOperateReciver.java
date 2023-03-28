package com.cj.im.service.message.mq;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cj.codec.proto.MessagePack;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.command.MessageCommand;
import com.cj.im.common.model.message.MessageContent;
import com.cj.im.common.model.message.MessageReadedContent;
import com.cj.im.common.model.message.MessageReciveAckContent;
import com.cj.im.service.message.service.MessageSyncService;
import com.cj.im.service.message.service.P2PMessageService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Component
public class ChatOperateReciver {
    private static final Log log = LogFactory.get();
    @Autowired
    private P2PMessageService p2PMessageService;

    @Autowired
    MessageSyncService messageSyncService;
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.Im2MessageService,durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2MessageService,durable = "true")

            ),concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String,Object> headers,
                              Channel channel) throws IOException {
        String msg = new String(message.getBody(),"utf-8");
        log.info("CHAT MSG FORM QUEUE ::: {}", msg);

        //一个deliver状态
        Long deliverTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            //单聊
            if(command.equals(MessageCommand.MSG_P2P.getCommand())) {
                MessageContent messageContent = JSON.toJavaObject(jsonObject, MessageContent.class);
                p2PMessageService.process(messageContent);
                //接受消息
            }else if(command.equals(MessageCommand.MSG_RECIVE_ACK.getCommand())){
                MessageReciveAckContent messageReciveAckContent;
                messageReciveAckContent = jsonObject.toJavaObject(MessageReciveAckContent.class);
                messageSyncService.receiveMark(messageReciveAckContent);


            }else if(command.equals(MessageCommand.MSG_READED.getCommand())){
                //消息已读
                MessageReadedContent messageReadedContent = JSON.toJavaObject(jsonObject, MessageReadedContent.class);
                messageSyncService.readMark(messageReadedContent);
            }
            channel.basicNack(deliverTag,false,false);
        } catch (Exception e) {
            log.info("向客户端发送消息失败");
            channel.basicNack(deliverTag,false,false);
        }

    }
}
