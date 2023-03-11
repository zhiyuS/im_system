package com.cj.im.tcp.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.cj.codec.proto.Message;
import com.cj.codec.proto.MessagePack;
import com.cj.im.common.constant.Constants;

import com.cj.im.tcp.consumer.process.BaseProcess;
import com.cj.im.tcp.consumer.process.ProcessFactory;
import com.cj.im.tcp.util.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

/**
 * 消费者 监听
 */
public class MqMessageConsumer {

    private static final Log log = LogFactory.get();
    private static String brokerId;
    private static void consumer(){
        try {

            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im+brokerId);
            //5.创建队列
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im+brokerId,true,false,false,null);

            /**
             * String queue,
             * String exchange,
             * String routingKey
             */
            channel.queueBind(Constants.RabbitConstants.MessageService2Im+brokerId,Constants.RabbitConstants.MessageService2Im,brokerId);

            channel.basicConsume(Constants.RabbitConstants.MessageService2Im+brokerId,false,
                    new DefaultConsumer(channel){

                //处理服务器发来的消息
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                    try {
                        String s = new String(body);
                        MessagePack messagePack = JSON.parseObject(s,MessagePack.class);

                        BaseProcess messageProcess = ProcessFactory.getMessageProcess(null);
                        messageProcess.process(messagePack);
                        /**
                         * 第一个参数
                         * 第二个参数 是否批量
                         * 第三参数 是否重回队列
                         */
                        log.info("消费了:"+s);
                        channel.basicNack(envelope.getDeliveryTag(), false,false);
                    } catch (IOException e) {
                        e.printStackTrace();
                        channel.basicNack(envelope.getDeliveryTag(), false,false);
                        log.info("消费失败");
                    }


                }
            });

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("监听失败");
        }

    }
    public static void init(){
        consumer();
    }
    public static void init(String brokerId){
        MqMessageConsumer.brokerId = brokerId;
        consumer();
    }

}
