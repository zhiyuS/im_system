package com.cj.im.service.friendship.controller;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.model.req.*;
import com.cj.im.service.friendship.service.ImFriendShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/friendship")
public class ImFriendShipController {
    @Autowired
    private ImFriendShipService imFriendShipService;

    /**
     * 导入好友
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("importFriendShip")
    public ResponseVO importFriendShip(@RequestBody @Validated ImportFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.importFriendShip(req);
    }

    /**
     * 添加好友
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.addFriend(req);
    }

    /**
     * 更新好友
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.updateFriend(req);
    }

    /**
     * 删除好友
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteFriend(req);
    }
    /**
     * 删除所有好友
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteAllFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteAllFriend(req);
    }
    @RequestMapping("getFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated GetFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.getFriend(req);
    }
    @RequestMapping("getAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated GetAllFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.getAllFriend(req);
    }
    @RequestMapping("checkFriend")
    public ResponseVO checkFriendSingle(@RequestBody @Validated CheckFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.checkFriend(req);
    }
    /**
     * 添加黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("addBlack")
    public ResponseVO addBlack(@RequestBody @Validated AddFriendShipBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.addBlack(req);
    }

    /**
     * 删除黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Validated DeleteBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteBlack(req);
    }
    /**
     * 检查黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("checkBlack")
    public ResponseVO checkBlack(@RequestBody @Validated CheckFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.checkFriendBlack(req);
    }


}
