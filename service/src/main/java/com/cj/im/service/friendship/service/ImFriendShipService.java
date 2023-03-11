package com.cj.im.service.friendship.service;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.model.req.*;

public interface ImFriendShipService {
    ResponseVO importFriendShip(ImportFriendShipReq req);

    ResponseVO addFriend(AddFriendShipReq req);
    ResponseVO updateFriend(UpdateFriendShipReq req);
    ResponseVO deleteFriend(DeleteFriendShipReq req);
    ResponseVO deleteAllFriend(DeleteAllFriendReq req);

    ResponseVO getFriend(GetFriendReq req);

    ResponseVO getAllFriend(GetAllFriendReq req);


    ResponseVO checkFriend(CheckFriendReq req);

    ResponseVO addBlack(AddFriendShipBlackReq req);

    ResponseVO deleteBlack(DeleteBlackReq req);

    ResponseVO checkFriendBlack(CheckFriendReq req);

}
