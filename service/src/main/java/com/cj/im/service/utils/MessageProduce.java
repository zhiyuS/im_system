package com.cj.im.service.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cj.codec.proto.MessagePack;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.command.Command;
import com.cj.im.common.model.ClientInfo;
import com.cj.im.common.model.UserSession;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MessageProduce {
    private static final Log log = LogFactory.get();

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    UserSessionUtils userSessionUtils;

    private String queueName = Constants.RabbitConstants.MessageService2Im;


    public boolean sendMessage(UserSession session,Object msg){
        try {
            log.info("send message == " + msg);
            rabbitTemplate.convertAndSend(queueName,session.getBrokerId()+"",msg);
            return true;
        }catch (Exception e){
            log.error("send error :" + e.getMessage());
            return false;
        }
    }

    //包装数据，调用sendMessage
    public boolean sendPack(String toId, Command command, Object msg, UserSession session){
        MessagePack messagePack = new MessagePack();
        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(session.getClientType());
        messagePack.setAppId(session.getAppId());
        messagePack.setImei(session.getImei());
        com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(com.alibaba.fastjson.JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);

        String body = JSONObject.toJSONString(messagePack);
        return sendMessage(session, body);
    }

    //发送给所有端的方法
    public List<ClientInfo> sendToUser(String toId, Command command, Object data, Integer appId){
        List<UserSession> userSession
                = userSessionUtils.getUserSession(appId, toId);
        List<ClientInfo> list = new ArrayList<>();
        for (UserSession session : userSession) {
            boolean b = sendPack(toId, command, data, session);
            if(b){
                list.add(new ClientInfo(session.getAppId(),session.getClientType(),session.getImei()));
            }
        }
        return list;
    }

    public void sendToUser(String toId, Integer clientType,String imei, Command command,
                           Object data, Integer appId){
        if(clientType != null && StrUtil.isNotBlank(imei)){
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUserExceptClient(toId,command,data,clientInfo);
        }else{
            sendToUser(toId,command,data,appId);
        }
    }

    //发送给某个用户的指定客户端
    public void sendToUser(String toId, Command command
            , Object data, ClientInfo clientInfo){
        UserSession userSession = userSessionUtils.getUserSession(clientInfo.getAppId(), toId, clientInfo.getClientType(),
                clientInfo.getImei());
        sendPack(toId,command,data,userSession);
    }

    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }

    //发送给除了某一端的其他端
    public void sendToUserExceptClient(String toId, Command command
            , Object data, ClientInfo clientInfo){
        List<UserSession> userSession = userSessionUtils
                .getUserSession(clientInfo.getAppId(),
                        toId);
        for (UserSession session : userSession) {
            if(!isMatch(session,clientInfo)){
                sendPack(toId,command,data,session);
            }
        }
    }
}
