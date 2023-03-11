package com.cj.im.service.friendship.service;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.cj.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;

public interface ImFriendShipGroupMemberService {
     ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

     ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);
     int doAddGroupMember(Long groupId, String toId);

     int clearGroupMember(Long groupId);

}
