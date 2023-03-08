package com.cj.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.cj.codec.config.BootstrapConfig;
import com.cj.codec.pack.LoginPack;
import com.cj.codec.proto.Message;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ImConnectStatusEnum;
import com.cj.im.common.enums.command.SystemCommand;
import com.cj.im.common.model.UserSession;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.net.InetAddress;

import static com.cj.im.common.enums.command.SystemCommand.*;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private Integer brokerId;
    public NettyServerHandler(Integer brokerId){
        this.brokerId = brokerId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = msg.getMessageHeader().getCommand();
        if(command == SystemCommand.LOGIN.getCommand()){

            LoginPack loginPack = JSONObject.parseObject(JSON.toJSONString(msg.getMessagePack()), LoginPack.class);


            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(loginPack.getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(loginPack.getClientType());

            UserSession userSession = new UserSession();
            userSession.setUserId(loginPack.getUserId());
            userSession.setAppId(msg.getMessageHeader().getAppId());
            userSession.setClientType(msg.getMessageHeader().getClientType());
            userSession.setConnectStatus(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setVersion(msg.getMessageHeader().getVersion());

            // 添加brokerId,brokerHost
            userSession.setBrokerId(brokerId);
            userSession.setBrokerHost(InetAddress.getLocalHost().getHostAddress());
            //当到redis
            RedissonClient redissonClient = RedisManage.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(msg.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(msg.getMessageHeader().getClientType()+"",JSON.toJSONString(userSession));

            SessionSocketHolder.put(loginPack.getUserId(), loginPack.getAppId(), loginPack.getClientType(),(NioSocketChannel)ctx.channel());

        }else if(command == SystemCommand.LOGOUT.getCommand()){
            //TODO 退出登录
            SessionSocketHolder.logout((NioSocketChannel) ctx.channel());
        }else if(command == SystemCommand.PING.getCommand()){
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
            int a = 1;
        }

    }

    /**
     * 触发读写超时时间会触发
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
