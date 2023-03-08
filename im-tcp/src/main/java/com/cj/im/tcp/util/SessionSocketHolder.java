package com.cj.im.tcp.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ImConnectStatusEnum;
import com.cj.im.common.model.UserClientDto;
import com.cj.im.common.model.UserSession;
import com.cj.im.tcp.redis.RedisManage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.Data;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {
    private static final Map<UserClientDto,NioSocketChannel> CHANNEL_MAP = new ConcurrentHashMap<>();

    public static void put(String userId,Integer appId,Integer clientType,NioSocketChannel channel){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        CHANNEL_MAP.put(userClientDto,channel);
    }
    public static NioSocketChannel get(String userId,Integer appId,Integer clientType){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);
        return CHANNEL_MAP.get(userClientDto);
    }
    public static void remove(String userId,Integer appId,Integer clientType){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);
        CHANNEL_MAP.remove(userClientDto);
    }
    public static void removeByValue(NioSocketChannel channel){

        CHANNEL_MAP.entrySet().stream().filter(e->(e.getValue() == channel)).forEach(e->{
            CHANNEL_MAP.remove(e.getKey());
        });
    }

    /**
     * 离线
     */
    public static void offline(NioSocketChannel channel){
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer)channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

        SessionSocketHolder.remove(userId,appId,clientType);

        //设置为离线
        RedissonClient redissonClient = RedisManage.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        if(StrUtil.isNotBlank(map.get(clientType))){
            UserSession userSession = JSON.parseObject(map.get(clientType), UserSession.class);
            userSession.setConnectStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType+"",JSON.toJSONString(userSession));
        }

    }

    /**
     * 退出登录
     */
    public static void logout(NioSocketChannel channel){
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer)channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

        SessionSocketHolder.remove(userId,appId,clientType);

        RedissonClient redissonClient = RedisManage.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType);
    }
}
