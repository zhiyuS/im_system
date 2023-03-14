package com.cj.im.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class ServiceApplicationTests {

	@Autowired
	StringRedisTemplate redisTemplate;
	@Test
	void contextLoads() {
	}
	@Test
	public void testr(){
		String key = "1000:seq:tt";
		String type = "friendShipReq";
		Long seq = 10L;
		redisTemplate.opsForHash().put(key,type,seq.toString());

	}

}
