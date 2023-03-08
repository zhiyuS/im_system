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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {
    private static final Map<UserClientDto,NioSocketChannel> CHANNEL_MAP = new ConcurrentHashMap<>();

    public static void put(String userId,Integer appId,Integer clientType,String imei,NioSocketChannel channel){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);

        CHANNEL_MAP.put(userClientDto,channel);
    }
    public static NioSocketChannel get(String userId,Integer appId,Integer clientType,String imei){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        return CHANNEL_MAP.get(userClientDto);
    }

    /**
     * 获取用户的端的信息
     * @param userId
     * @param appId
     */
    public static List<NioSocketChannel> get(String userId,String appId){
        Set<UserClientDto> userClientDtos = CHANNEL_MAP.keySet();
        List<NioSocketChannel> channels = new ArrayList<>();
        userClientDtos.forEach(u->{
            //判断appId 和 userId 与 userClientDto是否相等
            if(appId.equals(u.getAppId().toString()) && userId.equals(u.getUserId()) ){
                NioSocketChannel nioSocketChannel = CHANNEL_MAP.get(u);
                channels.add(nioSocketChannel);
            }
        });

        return  channels;
    }

    public static void remove(String userId,Integer appId,Integer clientType,String imei){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
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
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(userId,appId,clientType,imei);

        //设置为离线
        RedissonClient redissonClient = RedisManage.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        if(StrUtil.isNotBlank(map.get(clientType))){
            UserSession userSession = JSON.parseObject(map.get(clientType), UserSession.class);
            userSession.setConnectStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType+":"+imei,JSON.toJSONString(userSession));
        }

    }

    /**
     * 退出登录
     */
    public static void logout(NioSocketChannel channel){
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer)channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(userId,appId,clientType,imei);

        RedissonClient redissonClient = RedisManage.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType+":"+imei);
    }
}
