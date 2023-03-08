package com.cj.im.service.user.service;

import com.cj.im.common.ResponseVO;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.model.req.*;
import com.cj.im.service.user.model.resp.GetUserInfoResp;

public interface UserService {
    ResponseVO importUser(ImportUserReq req);
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId , Integer appId);

    public ResponseVO deleteUser(DeleteUserReq req);

    public ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    public ResponseVO login(LoginReq req);

    ResponseVO getUserSequence(GetUserSequenceReq req);
}
