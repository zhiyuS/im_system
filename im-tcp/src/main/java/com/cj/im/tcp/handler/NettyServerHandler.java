package com.cj.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.cj.codec.config.BootstrapConfig;
import com.cj.codec.pack.LoginPack;
import com.cj.codec.pack.message.ChatMessageAck;
import com.cj.codec.proto.Message;
import com.cj.codec.proto.MessagePack;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ImConnectStatusEnum;
import com.cj.im.common.enums.command.MessageCommand;
import com.cj.im.common.enums.command.SystemCommand;
import com.cj.im.common.model.UserClientDto;
import com.cj.im.common.model.UserSession;
import com.cj.im.common.model.message.CheckSendMessageReq;
import com.cj.im.tcp.feign.FeignMessageService;
import com.cj.im.tcp.publish.MqMessageProduce;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.util.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
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
    private FeignMessageService feignMessageService;
    public NettyServerHandler(Integer brokerId,String logicUrl){
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))//设置超时时间
                .target(FeignMessageService.class, logicUrl);
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
        }else if(command == MessageCommand.MSG_P2P.getCommand()){
            //调用接口
            CheckSendMessageReq checkSendMessageReq = new CheckSendMessageReq();
            checkSendMessageReq.setAppId(msg.getMessageHeader().getAppId());
            checkSendMessageReq.setCommand(msg.getMessageHeader().getCommand());
            JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));

            String fromId = jsonObject.getString("fromId");
            String toId = jsonObject.getString("toId");

            checkSendMessageReq.setToId(toId);
            checkSendMessageReq.setFromId(fromId);

            ResponseVO responseVO = feignMessageService.checkSendMessage(checkSendMessageReq);
            //成功 投递给mq

            if(responseVO.isOk()){

                MqMessageProduce.sendMessage(msg,command);
            }else{
                //失败 返回ack
                ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                responseVO.setData(chatMessageAck);
                MessagePack<ResponseVO> ack = new MessagePack<>();
                ack.setData(responseVO);
                ack.setCommand(MessageCommand.MSG_P2P.getCommand());
                ctx.channel().writeAndFlush(ack);
            }
        }else{
            MqMessageProduce.sendMessage(msg,command);

        }

    }

    /**
     * 离线状态
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionSocketHolder.offline((NioSocketChannel) ctx.channel());
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
