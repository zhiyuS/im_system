package com.cj.im.service.user.controller;

import com.cj.im.common.ClientType;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.route.RouteHandler;
import com.cj.im.common.route.RouteInfo;
import com.cj.im.service.user.model.req.GetUserInfoReq;
import com.cj.im.service.user.model.req.ImportUserReq;
import com.cj.im.service.user.model.req.LoginReq;
import com.cj.im.service.user.service.UserService;
import com.cj.im.service.utils.ZKit;
import com.cj.im.tcp.util.RouteInfoParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RouteHandler routeHandler;

    @Autowired
    private ZKit zKit;

    @PostMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req,Integer appId){
        req.setAppId(appId);
        return userService.importUser(req);
    }
    /**
     * im 登录接口
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq loginReq, Integer appId){

        ResponseVO login = userService.login(loginReq);
        List<String> nodes;
        //登录的逻辑
        if(login.isOk()){
            if(loginReq.getClientType() == ClientType.WEB.getCode()){
                 nodes = zKit.getAllWebNode();
            }else{
                nodes = zKit.getAllTcpNode();
            }
            //获取随机获取节点
            String routePath = routeHandler.routeServer(nodes, loginReq.getUserId());
            //解析
            RouteInfo parse = RouteInfoParseUtil.parse(routePath);

            return ResponseVO.successResponse(parse);
        }


        return null;
    }


}
