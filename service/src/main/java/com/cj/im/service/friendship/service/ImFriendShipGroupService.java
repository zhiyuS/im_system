package com.cj.im.service.friendship.service;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.cj.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.cj.im.service.friendship.model.req.DeleteFriendShipGroupReq;

public interface ImFriendShipGroupService {
     ResponseVO addGroup(AddFriendShipGroupReq req);

     ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

     ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

}
