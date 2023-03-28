package com.cj.im.tcp.consumer.process;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.cj.codec.proto.MessagePack;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 抽象类
 * 给客户端发消息
 */
public abstract class BaseProcess {
    private static final Log log =  LogFactory.get();
    public abstract void before();
    public void process(MessagePack messagePack){
        before();
        //根据toid，appId，等获取channel
        NioSocketChannel channel = SessionSocketHolder.get(messagePack.getToId(), messagePack.getAppId(), messagePack.getClientType(), messagePack.getImei());
        if(ObjectUtil.isNull(channel)){
            log.info("获取客户端失败");
            return;
        }
        //写入消息
        channel.writeAndFlush(messagePack);
        after();
    }
    public abstract void after();

}
