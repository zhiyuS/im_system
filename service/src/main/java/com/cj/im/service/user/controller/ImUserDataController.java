package com.cj.im.service.user.controller;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.user.model.req.DeleteUserReq;
import com.cj.im.service.user.model.req.GetUserInfoReq;
import com.cj.im.service.user.model.req.ModifyUserInfoReq;
import com.cj.im.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/user/data")
public class ImUserDataController {
    @Autowired
    private UserService userService;

    @PostMapping("getUserInfo")
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq userInfoReq,Integer appId){
        userInfoReq.setAppId(appId);
        return userService.getUserInfo(userInfoReq);
    }
    @PostMapping("deleteUser")
    public ResponseVO deleteUser(@RequestBody DeleteUserReq userReq,Integer appId){
        userReq.setAppId(appId);
        return userService.deleteUser(userReq);
    }
    @PostMapping("update")
    public ResponseVO updateUser(@RequestBody ModifyUserInfoReq userInfoReq,Integer appId){
        userInfoReq.setAppId(appId);
        return userService.modifyUserInfo(userInfoReq);
    }
}
