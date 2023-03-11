package com.cj.im.service.utils;


import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.utils.HttpRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class CallBackService {

    @Autowired
    private HttpRequestUtils httpRequestUtils;

    @Autowired
    private AppConfig appConfig;

    /**
     * 之后回调
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     */
    public void callback(Integer appId,String callbackCommand,String jsonBody){
        try {
            httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderMap(appId,callbackCommand),jsonBody,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 之前回调
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     * @return
     */
    public ResponseVO beforeCallback(Integer appId,String callbackCommand,String jsonBody){
        try {
            ResponseVO responseVO = httpRequestUtils.doPost(appConfig.getCallbackUrl(), ResponseVO.class, builderMap(appId, callbackCommand), jsonBody, null);
            return responseVO;
        } catch (Exception e) {
            System.out.println("callback 回调之前出现异常");
            return ResponseVO.successResponse();
        }
    }
    public Map builderMap(Integer appId, String callbackCommand){
        Map map = new HashMap<>();
        map.put("appId",appId);
        map.put("callbackCommand",callbackCommand);
        return map;

    }


}
