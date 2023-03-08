package com.cj.im.tcp.redis;

import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.consumer.UserLoginMessageLister;
import org.redisson.api.RedissonClient;

public class RedisManage {

    private static RedissonClient redissonClient;
    public static void init(BootstrapConfig config){

        redissonClient = new SingleClientStrategy().getRedissonClient(config.getIm().getRedis());
        UserLoginMessageLister userLoginMessageLister = new UserLoginMessageLister(config.getIm().getLoginModel());
        userLoginMessageLister.ListerUserLogin();

    }
    public static RedissonClient getRedissonClient(){
        return  redissonClient;
    }
}
