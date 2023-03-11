package com.cj.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cj.im.common.ResponseVO;
import com.cj.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.cj.im.service.friendship.dao.ImFriendShipGroupMemberEntity;
import com.cj.im.service.friendship.dao.mapper.ImFriendShipGroupMemberMapper;
import com.cj.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.cj.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.cj.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.cj.im.service.friendship.service.ImFriendShipGroupService;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImFriendShipGroupMemberServiceImpl implements ImFriendShipGroupMemberService {

    @Autowired
    ImFriendShipGroupMemberMapper imFriendShipGroupMemberMapper;

    @Lazy
    @Autowired
    ImFriendShipGroupService imFriendShipGroupService;

    @Autowired
    UserService imUserService;


    @Override
    @Transactional
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {

        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService
                .getGroup(req.getFromId(),req.getGroupName(),req.getAppId());
        if(!group.isOk()){
            return group;
        }

        List<String> successId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if(singleUserInfo.isOk()){
                int i = doAddGroupMember(group.getData().getGroupId(), toId);
                if(i == 1){
                    successId.add(toId);
                }
            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService
                .getGroup(req.getFromId(),req.getGroupName(),req.getAppId());
        if(!group.isOk()){
            return group;
        }

        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if(singleUserInfo.isOk()){
                int i = deleteGroupMember(group.getData().getGroupId(), req.getToIds());
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public int doAddGroupMember(Long groupId, String toId) {
        ImFriendShipGroupMemberEntity imFriendShipGroupMemberEntity = new ImFriendShipGroupMemberEntity();
        imFriendShipGroupMemberEntity.setGroupId(groupId);
        imFriendShipGroupMemberEntity.setToId(toId);
        try {
            int insert = imFriendShipGroupMemberMapper.insert(imFriendShipGroupMemberEntity);
            return insert;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    public int deleteGroupMember(Long groupId, List<String> toIds) {
        QueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id",groupId);
        queryWrapper.in("to_id",toIds);

        try {
            int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
//            int insert = imFriendShipGroupMemberMapper.insert(imFriendShipGroupMemberEntity);
            return delete;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int clearGroupMember(Long groupId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id",groupId);
        int delete = imFriendShipGroupMemberMapper.delete(query);
        return delete;
    }

}
