package com.cj.im.tcp.publish;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cj.codec.proto.Message;
import com.cj.im.common.constant.Constants;
import com.cj.im.tcp.util.MqFactory;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 发送消息
 */
public class MqMessageProduce {
    private static final Log log = LogFactory.get();
    public static void sendMessage(Message message,Integer command){

        Channel channel = null;
        String channelName = Constants.RabbitConstants.Im2MessageService;

        if(command.toString().startsWith("2")){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }
        try {
            channel = MqFactory.getChannel(channelName);

            JSONObject o = (JSONObject) JSON.toJSON(message.getMessagePack());
            o.put("command",command);
            o.put("clientType",message.getMessageHeader().getClientType());
            o.put("imei",message.getMessageHeader().getImei());
            o.put("appId",message.getMessageHeader().getAppId());
            channel.basicPublish(channelName,"",
                    null, o.toJSONString().getBytes());
            log.info("发送消息成功:{}",message);
        }catch (Exception e){
            log.error("发送消息出现异常：{}",e.getMessage());
        }
    }
}
