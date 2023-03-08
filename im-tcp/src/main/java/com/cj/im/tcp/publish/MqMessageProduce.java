package com.cj.im.tcp.publish;

import com.alibaba.fastjson.JSON;
import com.cj.im.tcp.util.MqFactory;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MqMessageProduce {
    public static void sendMessage(Object message){

        Channel channel = null;
        String channelName = "";
        String exchangeName = "";
        try {
            channel = MqFactory.getChannel(channelName);
            channel.basicPublish(exchangeName,"",null,JSON.toJSONBytes(message));
        } catch (Exception e) {
            System.out.println("发送消息出现异常");
        }
    }
}
