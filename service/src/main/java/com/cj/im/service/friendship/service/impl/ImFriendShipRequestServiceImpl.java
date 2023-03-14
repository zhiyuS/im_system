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
import com.cj.im.service.seq.RedisSeq;
import com.cj.im.service.utils.MessageProduce;
import com.cj.im.service.utils.WriteUserSeq;
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
    @Autowired
    RedisSeq redisSeq;

    @Autowired
    WriteUserSeq writeUserSeq;
    @Override
    public ResponseVO addFriendShipReq(String fromId, Integer appId, FriendDto dto) {

        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",dto.getToId());
        ImFriendShipRequestEntity request = friendShipRequestMapper.selectOne(queryWrapper);

        long seq = redisSeq.doGetSeq(appId+":"+
                Constants.SeqConstants.FriendshipRequest);


        if(ObjectUtil.isNull(request)){

            request = new ImFriendShipRequestEntity();
            request.setAddSource(dto.getAddSource());
            request.setAddWording(dto.getAddWording());
            request.setSequence(seq);
            request.setAppId(appId);
            request.setFromId(fromId);
            request.setToId(dto.getToId());
            request.setReadStatus(0);
            request.setApproveStatus(0);
            request.setRemark(dto.getRemark());
            request.setCreateTime(System.currentTimeMillis());
            friendShipRequestMapper.insert(request);

        }else{
            //修改记录内容 和更新时间
            if(StrUtil.isNotBlank(dto.getAddSource())){
                request.setAddWording(dto.getAddWording());
            }
            if(StrUtil.isNotBlank(dto.getRemark())){
                request.setRemark(dto.getRemark());
            }
            if(StrUtil.isNotBlank(dto.getAddWording())){
                request.setAddWording(dto.getAddWording());
            }
            request.setSequence(seq);
            friendShipRequestMapper.updateById(request);
        }
        writeUserSeq.writeUserSeq(appId,dto.getToId(),
                Constants.SeqConstants.FriendshipRequest,seq);

        //发送好友申请的tcp给接收方
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
            //只能审批发给自己的好友请求
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        long seq = redisSeq.doGetSeq(req.getAppId()+":"+
                Constants.SeqConstants.FriendshipRequest);

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setId(req.getId());
        update.setSequence(seq);

        friendShipRequestMapper.updateById(update);
        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperater(),
                Constants.SeqConstants.FriendshipRequest,seq);


        if(ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()){
            //同意 ===> 去执行添加好友逻辑
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

        //TCP发送添加好友请求给B
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

        long seq = redisSeq.doGetSeq(req.getAppId()+":"+
                Constants.SeqConstants.FriendshipRequest);
        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperater(),
                Constants.SeqConstants.FriendshipRequest,seq);

        //TCP通知
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(seq);
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
