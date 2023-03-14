package com.cj.im.service.message.service;


import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.enums.*;
import com.cj.im.service.friendship.dao.ImFriendShipEntity;
import com.cj.im.service.friendship.model.req.GetFriendReq;
import com.cj.im.service.friendship.model.req.GetRelationReq;
import com.cj.im.service.friendship.service.ImFriendShipService;
import com.cj.im.service.group.dao.ImGroupEntity;
import com.cj.im.service.group.model.resp.GetRoleInGroupResp;
import com.cj.im.service.group.service.ImGroupMemberService;
import com.cj.im.service.group.service.ImGroupService;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class CheckSendMessageService {

    @Autowired
    UserService imUserService;

    @Autowired
    ImFriendShipService imFriendService;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    @Autowired
    AppConfig appConfig;


    /**
     * 前置效验好友
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO checkSenderForvidAndMute(String fromId, Integer appId){

        ResponseVO<ImUserDataEntity> singleUserInfo
                = imUserService.getSingleUserInfo(fromId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        ImUserDataEntity user = singleUserInfo.getData();
        //是否被禁用
        if(user.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        }
        //是否被禁言
        if(user.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    public ResponseVO checkFriendShip(String fromId,String toId,Integer appId){

        if(appConfig.isSendMessageCheckFriend()){
            //A-B是否有好友关系
            GetFriendReq fromReq = new GetFriendReq();
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> fromRelation = imFriendService.getFriend(fromReq);
            if(!fromRelation.isOk()){
                return fromRelation;
            }
            //B-A是否有好友关系
            GetRelationReq toReq = new GetRelationReq();
            fromReq.setFromId(toId);
            fromReq.setToId(fromId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> toRelation = imFriendService.getFriend(fromReq);
            if(!toRelation.isOk()){
                return toRelation;
            }


            //A-B是否正常
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
            != fromRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
            //B-A是否正常
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != toRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }


            //是否开启效验
            if(appConfig.isSendMessageCheckBlack()){
                //A-B是否拉黑
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != fromRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }
                //B-A是否拉黑
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != toRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }

        return ResponseVO.successResponse();
    }
    public ResponseVO checkGroupMessage(String fromId,String groupId,Integer appId){

        ResponseVO responseVO = checkSenderForvidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }

        //判断群逻辑
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(groupId, appId);
        if(!group.isOk()){
            return group;
        }

        //判断群成员是否在群内
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if(!roleInGroupOne.isOk()){
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();

        //判断群是否被禁言
        //如果禁言 只有裙管理和群主可以发言
        ImGroupEntity groupData = group.getData();
        if(groupData.getMute() == GroupMuteTypeEnum.MUTE.getCode()
         && (data.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() ||
                data.getRole() == GroupMemberRoleEnum.OWNER.getCode()  )){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }

        if(data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }

        return ResponseVO.successResponse();
    }


}
