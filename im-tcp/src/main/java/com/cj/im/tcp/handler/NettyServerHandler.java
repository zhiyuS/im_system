package com.cj.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.cj.codec.pack.LoginPack;
import com.cj.codec.proto.Message;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ImConnectStatusEnum;
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

import static com.cj.im.common.enums.command.SystemCommand.LOGIN;
import static com.cj.im.common.enums.command.SystemCommand.LOGOUT;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = msg.getMessageHeader().getCommand();
        if(command == LOGIN.getCommand()){

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

            //当到redis
            RedissonClient redissonClient = RedisManage.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(msg.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(msg.getMessageHeader().getClientType()+"",JSON.toJSONString(msg));

            SessionSocketHolder.put(loginPack.getUserId(), loginPack.getAppId(), loginPack.getClientType(),(NioSocketChannel)ctx.channel());

        }else if(command == LOGOUT.getCommand()){
            //TODO 退出登录
            String userId = (String) ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).get();
            Integer appId = (Integer) ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).get();
            Integer clientType = (Integer)ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).get();

            SessionSocketHolder.remove(userId,appId,clientType);

            RedissonClient redissonClient = RedisManage.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
            map.remove(msg.getMessageHeader().getClientType());

        }

    }
}
