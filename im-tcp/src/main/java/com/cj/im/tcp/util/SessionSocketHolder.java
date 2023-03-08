package com.cj.im.tcp.util;

import com.cj.im.common.model.UserClientDto;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;

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
}
