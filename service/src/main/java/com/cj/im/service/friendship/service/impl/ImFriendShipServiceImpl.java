package com.cj.im.service.friendship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cj.codec.pack.friendship.*;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.*;
import com.cj.im.common.enums.command.FriendshipEventCommand;
import com.cj.im.common.model.RequestBase;
import com.cj.im.common.model.SyncReq;
import com.cj.im.common.model.SyncResp;
import com.cj.im.service.friendship.dao.ImFriendShipEntity;
import com.cj.im.service.friendship.dao.mapper.ImFriendShipMapper;
import com.cj.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.cj.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.cj.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.cj.im.service.friendship.model.req.*;
import com.cj.im.service.friendship.model.resp.CheckFriendShipResp;
import com.cj.im.service.friendship.model.resp.ImportFriendShipResp;
import com.cj.im.service.friendship.service.ImFriendShipRequestService;
import com.cj.im.service.friendship.service.ImFriendShipService;
import com.cj.im.service.seq.RedisSeq;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import com.cj.im.service.utils.CallBackService;
import com.cj.im.service.utils.MessageProduce;
import com.cj.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImFriendShipServiceImpl implements ImFriendShipService {

    @Resource
    ImFriendShipMapper friendShipMapper;

    @Autowired
    UserService userService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallBackService callbackService;

    @Lazy
    @Autowired
    ImFriendShipRequestService friendShipRequestService;


    @Autowired
    RedisSeq redisSeq;
    @Autowired
    MessageProduce messageProducer;

    @Autowired
    WriteUserSeq writeUserSeq;
    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {
        if(req.getFriendData().size() > 100){
            return ResponseVO.errorResponse(FriendShipErrorCode.IMPORT_SIZE_BEYOND);
        }
        List<ImportFriendShipReq.ImportFriendDto> list = req.getFriendData();

        ImportFriendShipResp importFriendShipResp = new ImportFriendShipResp();

        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();
        list.forEach(friend->{
            try {
                //插入第一条记录
                ImFriendShipEntity imFriendShipEntity = new ImFriendShipEntity();

                BeanUtil.copyProperties(friend,imFriendShipEntity);

                imFriendShipEntity.setAppId(req.getAppId());
                imFriendShipEntity.setFromId(req.getFromId());
                int insert = friendShipMapper.insert(imFriendShipEntity);

                if(insert == 1){
                    successId.add(friend.getToId());
                }else{
                    errorId.add(friend.getToId());
                }

            } catch (Exception e) {
                e.printStackTrace();
                errorId.add(friend.getToId());
            }
        });
        importFriendShipResp.setSuccessId(successId);
        importFriendShipResp.setErrorId(errorId);
        return ResponseVO.successResponse(importFriendShipResp);
    }

    /**
     * 添加好友
     * @param req
     * @return
     */
    @Override
    public ResponseVO addFriend(AddFriendShipReq req) {

        //判断用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!singleUserInfo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        ResponseVO<ImUserDataEntity> singleUserInfoTo = userService.getSingleUserInfo(req.getFriendDto().getToId(), req.getAppId());
        if(!singleUserInfoTo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }

        if (appConfig.isAddFriendBeforeCallback()) {
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }
        }

        ImUserDataEntity data = singleUserInfoTo.getData();
        //添加好友的逻辑
        if(ObjectUtil.isNotNull(data) && data.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()){
            return doAddFriend(req, req.getFriendDto(),req.getAppId(),req.getFromId());
        }else{
            ResponseVO responseVO = friendShipRequestService.addFriendShipReq(req.getFromId(), req.getAppId(), req.getFriendDto());
            if(!responseVO.isOk()){
               return responseVO;
            }
        }

        return ResponseVO.successResponse();

    }

    @Override
    public ResponseVO updateFriend(UpdateFriendShipReq req) {
        //判断用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!singleUserInfo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        ResponseVO<ImUserDataEntity> singleUserInfoTo = userService.getSingleUserInfo(req.getFriendDto().getToId(), req.getAppId());
        if(!singleUserInfoTo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        ResponseVO responseVO = doUpdateFriend(req.getFriendDto(), req.getAppId(), req.getFromId());
        if(responseVO.isOk()){

            //更新好友的TCP通知
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(req.getFriendDto().getRemark());
            updateFriendPack.setToId(req.getFriendDto().getToId());
            messageProducer.sendToUser(req.getFromId(),
                    req.getClientType(),req.getImei(),FriendshipEventCommand
                            .FRIEND_UPDATE,updateFriendPack,req.getAppId());


            //回调
            if (appConfig.isModifyFriendAfterCallback()){
                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToItem(req.getFriendDto());
                callbackService.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.UpdateFriendAfter, JSONObject
                                .toJSONString(callbackDto));
            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteFriend(DeleteFriendShipReq req) {
        //判断好友关系是否存在，判断是否已经添加
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);

        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }
        if(friendShipEntity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode()){
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
        }

        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);

        updateWrapper.lambda().set(ImFriendShipEntity::getStatus,FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode())
                .set(ImFriendShipEntity::getFriendSequence,seq)
                .eq(ImFriendShipEntity::getAppId,req.getAppId())
                .eq(ImFriendShipEntity::getFromId,req.getFromId())
                .eq(ImFriendShipEntity::getToId,req.getToId());
        friendShipMapper.update(null,updateWrapper);
        writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);

        //删除好友TCP通知
        DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
        deleteFriendPack.setSequence(seq);
        deleteFriendPack.setToId(req.getToId());
        messageProducer.sendToUser(req.getFromId(),
                req.getClientType(), req.getImei(),
                FriendshipEventCommand.FRIEND_DELETE,
                deleteFriendPack, req.getAppId());
        //之后回调
        if (appConfig.isAddFriendAfterCallback()){
            DeleteFriendAfterCallbackDto callbackDto = new DeleteFriendAfterCallbackDto();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.DeleteFriendAfter, JSONObject
                            .toJSONString(callbackDto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteAllFriend(DeleteAllFriendReq req) {
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getStatus,FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode())
                .eq(ImFriendShipEntity::getAppId,req.getAppId())
                .eq(ImFriendShipEntity::getFromId,req.getFromId());
        friendShipMapper.update(null,updateWrapper);

        //删除所有好友TCP通知
        DeleteAllFriendPack deleteFriendPack = new DeleteAllFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_ALL_DELETE,
                deleteFriendPack, req.getAppId());
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriend(GetFriendReq req) {

        //判断好友关系是否存在，判断是否已经添加
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);

        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }
        return ResponseVO.successResponse(friendShipEntity);
    }

    @Override
    public ResponseVO getAllFriend(GetAllFriendReq req) {
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        List<ImFriendShipEntity> friendShipEntityList = friendShipMapper.selectList(queryWrapper);

        return ResponseVO.successResponse(friendShipEntityList);
    }


    @Override
    public ResponseVO checkFriend(CheckFriendReq req) {
        //检查单个用户
        List<CheckFriendShipResp> resp;
        Map<String,Integer> result = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s->0));
        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp= friendShipMapper.checkFriendSingle(req);
        }else{
            resp = friendShipMapper.checkFriendBoth(req);
        }
        Map<String,Integer> collect = resp.stream().collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

        for(String toId : req.getToIds()){
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                resp.add(checkFriendShipResp);
            }
        }


        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {
        //判断用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!singleUserInfo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        ResponseVO<ImUserDataEntity> singleUserInfoTo = userService.getSingleUserInfo(req.getToId(), req.getAppId());
        if(!singleUserInfoTo.isOk()){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        return doAddBlack(req,req.getToId(),req.getAppId(),req.getFromId());
    }

    private ResponseVO doAddBlack(RequestBase req,String toId, Integer appId, String fromId) {
        //判断好友关系是否存在，判断是否已经添加
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",toId);

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);
        //好友关系不存在
        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }else{
            //好友关系存在，状态为已为黑名单
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            }
            //好友关系存在，状态为不是黑名单
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()){

                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);

                int update1 = friendShipMapper.update(update, queryWrapper);
                if(update1 != 1){
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
                }
                writeUserSeq.writeUserSeq(req.getAppId(),fromId,Constants.SeqConstants.Friendship,seq);

            }

        }

        //发送tcp通知
        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setFromId(fromId);
        addFriendBlackPack.setSequence(seq);
        addFriendBlackPack.setToId(toId);
        messageProducer.sendToUser(fromId, req.getClientType(), req.getImei(),

                FriendshipEventCommand.FRIEND_BLACK_ADD, addFriendBlackPack, req.getAppId());

        //之后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()){
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            callbackDto.setFromId(fromId);
            callbackDto.setToId(toId);
            callbackService.beforeCallback(appId,
                    Constants.CallbackCommand.AddBlackAfter, JSONObject
                            .toJSONString(callbackDto));
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        //判断好友关系是否存在，判断是否已经添加
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);

        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }
        //好友已被拉黑
        if(friendShipEntity.getStatus() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
        }

        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getBlack,FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode())
                .eq(ImFriendShipEntity::getAppId,req.getAppId())
                .eq(ImFriendShipEntity::getFromId,req.getFromId())
                .eq(ImFriendShipEntity::getToId,req.getToId());
        int update = friendShipMapper.update(null, updateWrapper);


        if(update == 1){

            //删除黑名单回调
            long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
            writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);
            DeleteBlackPack deleteFriendPack = new DeleteBlackPack();
            deleteFriendPack.setFromId(req.getFromId());
            deleteFriendPack.setSequence(seq);
            deleteFriendPack.setToId(req.getToId());
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
                    deleteFriendPack, req.getAppId());
            //之后回调
            if (appConfig.isAddFriendShipBlackAfterCallback()){
                AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToId(req.getToId());
                callbackService.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.DeleteBlack, JSONObject
                                .toJSONString(callbackDto));
            }
        }
        return ResponseVO.successResponse();
    }

    /**
     * 检查黑名单
     * @param req
     * @return
     */
    @Override
    public ResponseVO checkFriendBlack(CheckFriendReq req) {
        //检查单个用户
        List<CheckFriendShipResp> resp;
        Map<String,Integer> result = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s->0));
        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp= friendShipMapper.checkFriendBlackSingle(req);
        }else{
            resp = friendShipMapper.checkFriendBlackBoth(req);
        }
        Map<String,Integer> collect = resp.stream().collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

        for(String toId : req.getToIds()){
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                resp.add(checkFriendShipResp);
            }
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO syncFriendshipList(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        SyncResp<ImFriendShipEntity> resp = new SyncResp<>();
        //seq > req.getseq limit maxLimit
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_id",req.getOperater());
        queryWrapper.gt("friend_sequence",req.getLastSequence());
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByAsc("friend_sequence");
        List<ImFriendShipEntity> list = friendShipMapper.selectList(queryWrapper);

        if(!CollectionUtils.isEmpty(list)){
            ImFriendShipEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            //设置最大seq
            Long friendShipMaxSeq = friendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperater());
            resp.setMaxSequence(friendShipMaxSeq);
            //设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }


    @Transactional
    ResponseVO doUpdateFriend(FriendDto dto, Integer appId, String fromId) {
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);


        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource,dto.getAddSource())
                .set(ImFriendShipEntity::getExtra,dto.getExtra())
                .set(ImFriendShipEntity::getRemark,dto.getRemark())
                .set(ImFriendShipEntity::getToId,dto.getToId())
                .set(ImFriendShipEntity::getFriendSequence,seq)
                .eq(ImFriendShipEntity::getAppId,appId)
                .eq(ImFriendShipEntity::getToId,dto.getToId())
                .eq(ImFriendShipEntity::getFromId,fromId);
        int update = friendShipMapper.update(null, updateWrapper);

        if(update == 1){
            writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);

            return ResponseVO.successResponse();
        }

        return ResponseVO.errorResponse();


    }

    @Transactional
    ResponseVO doAddFriend(RequestBase requestBase, FriendDto dto, Integer appId, String fromId) {

        //判断好友关系是否存在，判断是否已经添加
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",dto.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);
        //好友关系不存在
        long seq = 0l;
        if(ObjectUtil.isNull(friendShipEntity)){
            //添加好友
            friendShipEntity = new ImFriendShipEntity();
            BeanUtil.copyProperties(dto,friendShipEntity);

            //添加seq
            seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);

            writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);

            friendShipEntity.setAppId(appId);
            friendShipEntity.setFromId(fromId);
            friendShipEntity.setFriendSequence(seq);
            friendShipEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            friendShipEntity.setCreateTime(System.currentTimeMillis());

            int insert = friendShipMapper.insert(friendShipEntity);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
        }else{
            //好友关系存在，状态为正常
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }
            //好友关系存在，状态为删除
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode()){

                ImFriendShipEntity update = new ImFriendShipEntity();
                if(StrUtil.isNotBlank(dto.getAddSource())){
                    update.setAddSource(dto.getAddSource());
                }
                if(StrUtil.isNotBlank(dto.getToId())){
                    update.setAddSource(dto.getToId());
                }
                if(StrUtil.isNotBlank(dto.getRemark())){
                    update.setAddSource(dto.getRemark());
                }
                if(StrUtil.isNotBlank(dto.getExtra())){
                    update.setAddSource(dto.getExtra());
                }
                seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
                writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);

                update.setFriendSequence(seq);
                int update1 = friendShipMapper.update(update, queryWrapper);
                if(update1 != 1){
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
            }

        }

        //添加B-A
        QueryWrapper<ImFriendShipEntity> toQuery = new QueryWrapper<>();
        toQuery.eq("app_id",appId);
        toQuery.eq("from_id",dto.getToId());
        toQuery.eq("to_id",fromId);
        ImFriendShipEntity toItem = friendShipMapper.selectOne(toQuery);
        if(toItem == null){
            toItem = new ImFriendShipEntity();
            toItem.setAppId(appId);
            toItem.setFromId(dto.getToId());


            BeanUtils.copyProperties(dto,toItem);
            toItem.setToId(fromId);
            toItem.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            toItem.setCreateTime(System.currentTimeMillis());
            toItem.setFriendSequence(seq);
//            toItem.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            int insert = friendShipMapper.insert(toItem);
            writeUserSeq.writeUserSeq(appId,dto.getToId(),Constants.SeqConstants.Friendship,seq);

        }else{
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() !=
                    toItem.getStatus()){
                ImFriendShipEntity update = new ImFriendShipEntity();
                writeUserSeq.writeUserSeq(appId,dto.getToId(),Constants.SeqConstants.Friendship,seq);

                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                friendShipMapper.update(update,toQuery);
            }
        }


        //发送给fromId
        AddFriendPack addFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(friendShipEntity,addFriendPack);

        if(requestBase != null){
            messageProducer.sendToUser(fromId,requestBase.getClientType(),
                    requestBase.getImei(), FriendshipEventCommand.FRIEND_ADD,addFriendPack
                    ,requestBase.getAppId());
        }else {
            messageProducer.sendToUser(fromId,
                    FriendshipEventCommand.FRIEND_ADD,addFriendPack
                    ,requestBase.getAppId());
        }

        //发送给toId
        AddFriendPack addFriendToPack = new AddFriendPack();
        BeanUtils.copyProperties(toItem,addFriendPack);
        addFriendPack.setSequence(seq);
        messageProducer.sendToUser(toItem.getFromId(),
                FriendshipEventCommand.FRIEND_ADD,addFriendToPack
                ,requestBase.getAppId());


        //之后回调
        if(appConfig.isAddFriendAfterCallback()){
            AddFriendAfterCallbackDto addFriendAfterCallbackDto = new AddFriendAfterCallbackDto();
            addFriendAfterCallbackDto.setFromId(fromId);
            addFriendAfterCallbackDto.setToItem(dto);
            callbackService.callback(appId,Constants.CallbackCommand.AddFriendAfter, JSON.toJSONString(addFriendAfterCallbackDto));
        }

        return ResponseVO.successResponse();
    }
}
