package com.cj.im.tcp.redis;

import com.cj.codec.config.BootstrapConfig;
import org.redisson.api.RedissonClient;

public class RedisManage {

    private static RedissonClient redissonClient;
    public static void init(BootstrapConfig config){

        redissonClient = new SingleClientStrategy().getRedissonClient(config.getIm().getRedis());
    }
    public static RedissonClient getRedissonClient(){
        return  redissonClient;
    }
}
