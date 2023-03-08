package com.cj.im.tcp.consumer;

import com.cj.im.common.constant.Constants;
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

            channel.basicConsume(Constants.RabbitConstants.MessageService2Im+brokerId,false,new DefaultConsumer(channel){

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                    System.out.println(new String(body));
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
