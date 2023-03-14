package com.cj.im.service.utils;

import com.cj.im.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class WriteUserSeq {

    //redis
    //uid friend 10
    //    group 12
    //    conversation 123
    @Autowired
    StringRedisTemplate redisTemplate;

    public void writeUserSeq(Integer appId,String userId,String type,Long seq){
        String key = appId + ":" + Constants.RedisConstants.SeqPrefix + ":" + userId;
//        redisTemplate.opsForHash().put(key,type,seq);
        redisTemplate.opsForHash().put(key,type,seq.toString());
    }


}
