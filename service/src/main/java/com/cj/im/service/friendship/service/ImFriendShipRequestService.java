package com.cj.im.service.friendship.service;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.model.req.ApproverFriendRequestReq;
import com.cj.im.service.friendship.model.req.FriendDto;
import com.cj.im.service.friendship.model.req.ReadFriendShipRequestReq;

public interface ImFriendShipRequestService {


    ResponseVO addFriendShipReq(String fromId, Integer appId, FriendDto friendDto);
    ResponseVO approverFriendRequest(ApproverFriendRequestReq req);

    ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);

    ResponseVO getFriendRequest(String fromId, Integer appId);
}
