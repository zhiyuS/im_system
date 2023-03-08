package com.cj.im.tcp;


import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

public class RedissenTest {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://42.193.140.45:6378");
        StringCodec stringCodec = new StringCodec();
        config.setCodec(stringCodec);
        RedissonClient redisson = Redisson.create(config);
        RBucket<Object> im = redisson.getBucket("im");
        System.out.println(im.get());
        im.set("im");
        System.out.println(im.get());
    }
}
