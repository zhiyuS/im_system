package com.cj.im.tcp.consumer.process;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.cj.codec.proto.MessagePack;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class BaseProcess {
    private static final Log log =  LogFactory.get();
    public abstract void before();
    public void process(MessagePack messagePack){
        before();
        NioSocketChannel channel = SessionSocketHolder.get(messagePack.getToId(), messagePack.getAppId(), messagePack.getClientType(), messagePack.getImei());
        if(ObjectUtil.isNull(channel)){
            log.info("获取客户端失败");
            return;
        }
        channel.writeAndFlush(messagePack);
        after();
    }
    public abstract void after();

}
