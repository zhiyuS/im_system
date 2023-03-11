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
import com.cj.im.common.model.UserClientDto;
import com.cj.im.common.model.UserSession;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
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
        /**
         * 登录
         */
        if(command == SystemCommand.LOGIN.getCommand()){

            LoginPack loginPack = JSONObject.parseObject(JSON.toJSONString(msg.getMessagePack()), LoginPack.class);


            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(msg.getMessageHeader().getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(msg.getMessageHeader().getClientType());
            ctx.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(msg.getMessageHeader().getImei());

            //设置基本信息
            UserSession userSession = new UserSession();
            userSession.setUserId(loginPack.getUserId());
            userSession.setAppId(msg.getMessageHeader().getAppId());
            userSession.setClientType(msg.getMessageHeader().getClientType());
            userSession.setImei(msg.getMessageHeader().getImei());

            //设置在线状态
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setVersion(msg.getMessageHeader().getVersion());

            // 添加brokerId,brokerHost
            userSession.setBrokerId(brokerId);
            userSession.setBrokerHost(InetAddress.getLocalHost().getHostAddress());

            //当到redis
            RedissonClient redissonClient = RedisManage.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(msg.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(msg.getMessageHeader().getClientType()+":"+msg.getMessageHeader().getImei(),JSON.toJSONString(userSession));

            int a = 1;
            /**
             * 发送登录的消息给redis
             */
            //1.设置消息
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setImei(msg.getMessageHeader().getImei());
            userClientDto.setClientType(msg.getMessageHeader().getClientType());
            userClientDto.setAppId(msg.getMessageHeader().getAppId());
            userClientDto.setUserId(loginPack.getUserId());

            //2.发送消息
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSON.toJSONString(userClientDto));

            //将用户存入SessionSocketHolder
            SessionSocketHolder.put(loginPack.getUserId(), msg.getMessageHeader().getAppId(), msg.getMessageHeader().getClientType(),msg.getMessageHeader().getImei(),(NioSocketChannel)ctx.channel());

        }else if(command == SystemCommand.LOGOUT.getCommand()){
            //TODO 退出登录
            SessionSocketHolder.logout((NioSocketChannel) ctx.channel());
        }else if(command == SystemCommand.PING.getCommand()){
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
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
