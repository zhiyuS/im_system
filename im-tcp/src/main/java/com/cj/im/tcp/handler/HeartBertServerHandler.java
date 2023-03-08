package com.cj.im.tcp.handler;

import com.cj.im.common.constant.Constants;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

public class HeartBertServerHandler extends ChannelInboundHandlerAdapter {
    private Integer heartBertTimeOut;

    public HeartBertServerHandler(Integer heartBertTimeOut) {
        this.heartBertTimeOut = heartBertTimeOut;
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //判断是否超时
        IdleStateEvent evt1 = (IdleStateEvent) evt;
        if(evt1.state() == IdleState.READER_IDLE){
            System.out.println("读超时");
        }else if(evt1.state() == IdleState.WRITER_IDLE){
            System.out.println("写超时");
        }else if(evt1.state() == IdleState.ALL_IDLE){
            //判断最后一次读写时间是否超过超时时间
            long readTime = (Long) ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).get();

            if(System.currentTimeMillis() - readTime > heartBertTimeOut){
                //TODO 离线
                 SessionSocketHolder.offline((NioSocketChannel) ctx.channel());
            }

        }

    }
}
