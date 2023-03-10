package com.cj.im.service.friendship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cj.codec.pack.friendship.ApproverFriendRequestPack;
import com.cj.codec.pack.friendship.ReadAllFriendRequestPack;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.ApproverFriendRequestStatusEnum;
import com.cj.im.common.enums.FriendShipErrorCode;
import com.cj.im.common.enums.command.FriendshipEventCommand;
import com.cj.im.common.exception.ApplicationException;
import com.cj.im.service.friendship.dao.ImFriendShipRequestEntity;
import com.cj.im.service.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.cj.im.service.friendship.model.req.ApproverFriendRequestReq;
import com.cj.im.service.friendship.model.req.FriendDto;
import com.cj.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.cj.im.service.friendship.service.ImFriendShipRequestService;
import com.cj.im.service.utils.MessageProduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {


    @Resource
    ImFriendShipRequestMapper friendShipRequestMapper;

    @Autowired
    ImFriendShipServiceImpl friendShipService;

    @Autowired
    MessageProduce messageProducer;

    @Override
    public ResponseVO addFriendShipReq(String fromId, Integer appId, FriendDto dto) {

        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",dto.getToId());
        ImFriendShipRequestEntity request = friendShipRequestMapper.selectOne(queryWrapper);

        if(ObjectUtil.isNull(request)){
            ImFriendShipRequestEntity imFriendShipRequestEntity = new ImFriendShipRequestEntity();

            BeanUtil.copyProperties(dto,imFriendShipRequestEntity);
            imFriendShipRequestEntity.setAppId(appId);
            imFriendShipRequestEntity.setFromId(fromId);

            int insert = friendShipRequestMapper.insert(imFriendShipRequestEntity);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
        }else{
            //?????????????????? ???????????????
            if(StrUtil.isNotBlank(dto.getAddSource())){
                request.setAddWording(dto.getAddWording());
            }
            if(StrUtil.isNotBlank(dto.getRemark())){
                request.setRemark(dto.getRemark());
            }
            if(StrUtil.isNotBlank(dto.getAddWording())){
                request.setAddWording(dto.getAddWording());
            }
            friendShipRequestMapper.updateById(request);
        }
//        writeUserSeq.writeUserSeq(appId,dto.getToId(),
//                Constants.SeqConstants.FriendshipRequest,seq);

        //?????????????????????tcp????????????
        messageProducer.sendToUser(dto.getToId(),
                null, "", FriendshipEventCommand.FRIEND_REQUEST,
                request, appId);


        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO approverFriendRequest(ApproverFriendRequestReq req) {
        ImFriendShipRequestEntity imFriendShipRequestEntity = friendShipRequestMapper.selectById(req.getId());
        if(imFriendShipRequestEntity == null){
            throw new ApplicationException(FriendShipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }

        if(!req.getOperater().equals(imFriendShipRequestEntity.getToId())){
            //???????????????????????????????????????
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }


        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setId(req.getId());
        friendShipRequestMapper.updateById(update);


        if(ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()){
            //?????? ===> ???????????????????????????
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWording(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = friendShipService.doAddFriend(req,dto, req.getAppId(), imFriendShipRequestEntity.getFromId());
//            if(!responseVO.isOk()){
////                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                return responseVO;
//            }
            if(!responseVO.isOk() && responseVO.getCode() != FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()){
                return responseVO;
            }
        }

        //TCP???????????????????????????B
        ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
        approverFriendRequestPack.setId(req.getId());
//        approverFriendRequestPack.setSequence(seq);
        approverFriendRequestPack.setStatus(req.getStatus());

        messageProducer.sendToUser(imFriendShipRequestEntity.getToId(),req.getClientType(),req.getImei(), FriendshipEventCommand
                .FRIEND_REQUEST_APPROVER,approverFriendRequestPack,req.getAppId());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req) {
        QueryWrapper<ImFriendShipRequestEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("to_id", req.getFromId());


        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
        friendShipRequestMapper.update(update, query);

        //TCP??????
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
//        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendToUser(req.getFromId(),req.getClientType(),req.getImei(),FriendshipEventCommand
                .FRIEND_REQUEST_READ,readAllFriendRequestPack,req.getAppId());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {
        QueryWrapper<ImFriendShipRequestEntity> query = new QueryWrapper();
        query.eq("app_id", appId);
        query.eq("to_id", fromId);

        List<ImFriendShipRequestEntity> requestList = friendShipRequestMapper.selectList(query);

        return ResponseVO.successResponse(requestList);
    }
}
