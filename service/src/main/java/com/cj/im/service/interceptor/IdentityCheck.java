package com.cj.im.service.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cj.im.common.BaseErrorCode;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.GateWayErrorCode;
import com.cj.im.common.exception.ApplicationExceptionEnum;
import com.cj.im.common.utils.SigAPI;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @description: 检查秘钥是否匹配
 * @author: cj
 * @version: 1.0
 */
@Component
public class IdentityCheck {

    private static Logger logger = LoggerFactory.getLogger(IdentityCheck.class);

    @Autowired
    UserService imUserService;

    //10000 123456 10001 123456789
    @Autowired
    AppConfig appConfig;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public ApplicationExceptionEnum checkUserSig(String identifier,
                                                 String appId, String userSig){

        String cacheUserSig = stringRedisTemplate.opsForValue()
                .get(appId + ":" + Constants.RedisConstants.userSign + ":"
                + identifier + userSig);
        if(!StrUtil.isBlank(cacheUserSig) && Long.valueOf(cacheUserSig)
         >  System.currentTimeMillis() / 1000){
            return BaseErrorCode.SUCCESS;
        }

        //获取秘钥
        String privateKey = appConfig.getPrivateKey();

        //根据appid + 秘钥创建sigApi
        SigAPI sigAPI = new SigAPI(Long.valueOf(appId), privateKey);

        //调用sigApi对userSig解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSig);

        //取出解密后的appid 和 操作人 和 过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decoerAppId = "";
        String decoderidentifier = "";

        try {
            decoerAppId = jsonObject.getString("TLS.appId");
            decoderidentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            expireSec = Long.valueOf(expireStr);
            expireTime = Long.valueOf(expireTimeStr) + expireSec;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("checkUserSig-error:{}",e.getMessage());
        }

        if(!decoderidentifier.equals(identifier)){
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        if(!decoerAppId.equals(appId)){
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        if(expireSec == 0L){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if(expireTime < System.currentTimeMillis() / 1000){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        //appid + "xxx" + userId + sign
//        String s = sigAPI.genUserSig(identifier, expireSec,time, null);

        String key = appId + ":" + Constants.RedisConstants.userSign + ":"
                +identifier + userSig;

        Long etime = expireTime - System.currentTimeMillis() / 1000;
        stringRedisTemplate.opsForValue().set(
                key,expireTime.toString(),etime, TimeUnit.SECONDS
        );
        setIsAdmin(identifier,Integer.valueOf(appId));
        return BaseErrorCode.SUCCESS;

    }

    private void setIsAdmin(String identifier, Integer appId) {
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(identifier, appId);

        if(singleUserInfo.isOk()){
            RequestHolder.set(singleUserInfo.getData().getUserType() == 4);
        }else{
            RequestHolder.set(false);
        }
    }


}
