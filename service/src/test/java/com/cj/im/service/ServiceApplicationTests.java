package com.cj.im.service;

import com.alibaba.fastjson.JSONObject;
import com.cj.im.common.utils.SigAPI;
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
	public void genUserSign(){
		//此处秘钥和app秘钥是一样的
		SigAPI asd = new SigAPI(1000, "123456");
		//设置多久之后过期 单位毫秒
		String sign = asd.genUserSig("cj", 10000000);
//        Thread.sleep(2000L);
		JSONObject jsonObject = asd.decodeUserSig(sign);
		System.out.println("sign:" + sign);
		System.out.println("decoder:" + jsonObject.toString());
	}
	@Test
	public void testr(){
		String key = "1000:seq:tt";
		String type = "friendShipReq";
		Long seq = 10L;
		redisTemplate.opsForHash().put(key,type,seq.toString());

	}

}
