package com.cj.im.service.user.controller;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.user.model.req.GetUserInfoReq;
import com.cj.im.service.user.model.req.ImportUserReq;
import com.cj.im.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Autowired
    private UserService userService;
    @PostMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req,Integer appId){
//        req.setAppId(appId);
        int a = 1;
        return userService.importUser(req);
    }

}
