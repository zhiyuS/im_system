package com.cj.im.service.friendship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cj.codec.pack.friendship.*;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.*;
import com.cj.im.common.enums.command.FriendshipEventCommand;
import com.cj.im.common.model.RequestBase;
import com.cj.im.service.friendship.dao.ImFriendShipEntity;
import com.cj.im.service.friendship.dao.mapper.ImFriendShipMapper;
import com.cj.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.cj.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.cj.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.cj.im.service.friendship.model.req.*;
import com.cj.im.service.friendship.model.resp.CheckFriendResp;
import com.cj.im.service.friendship.model.resp.ImportFriendShipResp;
import com.cj.im.service.friendship.service.ImFriendShipRequestService;
import com.cj.im.service.friendship.service.ImFriendShipService;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.service.UserService;
import com.cj.im.service.user.service.impl.UserServiceImpl;
import com.cj.im.service.utils.CallBackService;
import com.cj.im.service.utils.MessageProduce;
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
    MessageProduce messageProducer;
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
                //?????????????????????
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
     * ????????????
     * @param req
     * @return
     */
    @Override
    public ResponseVO addFriend(AddFriendShipReq req) {

        //????????????????????????
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

        ImUserDataEntity data = singleUserInfo.getData();
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
        //????????????????????????
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

            //???????????????TCP??????
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(req.getFriendDto().getRemark());
            updateFriendPack.setToId(req.getFriendDto().getToId());
            messageProducer.sendToUser(req.getFromId(),
                    req.getClientType(),req.getImei(),FriendshipEventCommand
                            .FRIEND_UPDATE,updateFriendPack,req.getAppId());


            //??????
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
        //?????????????????????????????????????????????????????????
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
        updateWrapper.lambda().set(ImFriendShipEntity::getStatus,FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode())
                .eq(ImFriendShipEntity::getAppId,req.getAppId())
                .eq(ImFriendShipEntity::getFromId,req.getFromId())
                .eq(ImFriendShipEntity::getToId,req.getToId());
        friendShipMapper.update(null,updateWrapper);


        //????????????TCP??????
        DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
//        deleteFriendPack.setSequence(seq);
        deleteFriendPack.setToId(req.getToId());
        messageProducer.sendToUser(req.getFromId(),
                req.getClientType(), req.getImei(),
                FriendshipEventCommand.FRIEND_DELETE,
                deleteFriendPack, req.getAppId());
        //????????????
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

        //??????????????????TCP??????
        DeleteAllFriendPack deleteFriendPack = new DeleteAllFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_ALL_DELETE,
                deleteFriendPack, req.getAppId());
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriend(GetFriendReq req) {

        //?????????????????????????????????????????????????????????
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
        //??????????????????
        List<CheckFriendResp> resp;
        Map<String,Integer> result = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s->0));
        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp= friendShipMapper.checkFriendSingle(req);
        }else{
            resp = friendShipMapper.checkFriendBoth(req);
        }
        Map<String,Integer> collect = resp.stream().collect(Collectors.toMap(CheckFriendResp::getToId,CheckFriendResp::getStatus));

        for(String toId : req.getToIds()){
            if(!collect.containsKey(toId)){
                CheckFriendResp checkFriendResp = new CheckFriendResp();
                checkFriendResp.setFromId(req.getFromId());
                checkFriendResp.setToId(toId);
                checkFriendResp.setStatus(result.get(toId));
                resp.add(checkFriendResp);
            }
        }


        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {
        //????????????????????????
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
        //?????????????????????????????????????????????????????????
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",toId);

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);
        //?????????????????????
        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }else{
            //?????????????????????????????????????????????
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            }
            //?????????????????????????????????????????????
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()){

                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());

                int update1 = friendShipMapper.update(update, queryWrapper);
                if(update1 != 1){
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
                }
            }

        }

        //??????tcp??????
        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setFromId(fromId);
//        addFriendBlackPack.setSequence(seq);
        addFriendBlackPack.setToId(toId);
        messageProducer.sendToUser(fromId, req.getClientType(), req.getImei(),

                FriendshipEventCommand.FRIEND_BLACK_ADD, addFriendBlackPack, req.getAppId());

        //????????????
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
        //?????????????????????????????????????????????????????????
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);

        if(ObjectUtil.isNull(friendShipEntity)){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }
        //??????????????????
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

            //?????????????????????
//            writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);
            DeleteBlackPack deleteFriendPack = new DeleteBlackPack();
            deleteFriendPack.setFromId(req.getFromId());
//            deleteFriendPack.setSequence(seq);
            deleteFriendPack.setToId(req.getToId());
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
                    deleteFriendPack, req.getAppId());
            //????????????
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
     * ???????????????
     * @param req
     * @return
     */
    @Override
    public ResponseVO checkFriendBlack(CheckFriendReq req) {
        //??????????????????
        List<CheckFriendResp> resp;
        Map<String,Integer> result = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s->0));
        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp= friendShipMapper.checkFriendBlackSingle(req);
        }else{
            resp = friendShipMapper.checkFriendBlackBoth(req);
        }
        Map<String,Integer> collect = resp.stream().collect(Collectors.toMap(CheckFriendResp::getToId,CheckFriendResp::getStatus));

        for(String toId : req.getToIds()){
            if(!collect.containsKey(toId)){
                CheckFriendResp checkFriendResp = new CheckFriendResp();
                checkFriendResp.setFromId(req.getFromId());
                checkFriendResp.setToId(toId);
                checkFriendResp.setStatus(result.get(toId));
                resp.add(checkFriendResp);
            }
        }
        return ResponseVO.successResponse(resp);
    }


    @Transactional
    ResponseVO doUpdateFriend(FriendDto dto, Integer appId, String fromId) {
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource,dto.getAddSource())
                .set(ImFriendShipEntity::getExtra,dto.getExtra())
                .set(ImFriendShipEntity::getRemark,dto.getRemark())
                .set(ImFriendShipEntity::getToId,dto.getToId())
                .eq(ImFriendShipEntity::getAppId,appId)
                .eq(ImFriendShipEntity::getToId,dto.getToId())
                .eq(ImFriendShipEntity::getFromId,fromId);
        int update = friendShipMapper.update(null, updateWrapper);

        if(update == 1){
            return ResponseVO.successResponse();
        }

        return ResponseVO.errorResponse();


    }

    @Transactional
    ResponseVO doAddFriend(RequestBase requestBase, FriendDto dto, Integer appId, String fromId) {

        //?????????????????????????????????????????????????????????
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id",appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",dto.getToId());

        ImFriendShipEntity friendShipEntity = friendShipMapper.selectOne(queryWrapper);
        //?????????????????????
        if(ObjectUtil.isNull(friendShipEntity)){
            //????????????
            ImFriendShipEntity newFriendShipEntity = new ImFriendShipEntity();
            BeanUtil.copyProperties(dto,newFriendShipEntity);

            newFriendShipEntity.setAppId(appId);
            newFriendShipEntity.setFromId(fromId);
            newFriendShipEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            newFriendShipEntity.setCreateTime(System.currentTimeMillis());

            int insert = friendShipMapper.insert(newFriendShipEntity);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
        }else{
            //????????????????????????????????????
            if(friendShipEntity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }
            //????????????????????????????????????
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
                int update1 = friendShipMapper.update(update, queryWrapper);
                if(update1 != 1){
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
            }

        }
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
//            toItem.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            int insert = friendShipMapper.insert(toItem);
        }else{
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() !=
                    toItem.getStatus()){
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                friendShipMapper.update(update,toQuery);
            }
        }


        //?????????from
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

        AddFriendPack addFriendToPack = new AddFriendPack();
        BeanUtils.copyProperties(toItem,addFriendPack);
        messageProducer.sendToUser(toItem.getFromId(),
                FriendshipEventCommand.FRIEND_ADD,addFriendToPack
                ,requestBase.getAppId());


        //????????????
        if(appConfig.isAddFriendAfterCallback()){
            AddFriendAfterCallbackDto addFriendAfterCallbackDto = new AddFriendAfterCallbackDto();
            addFriendAfterCallbackDto.setFromId(fromId);
            addFriendAfterCallbackDto.setToItem(dto);
            callbackService.callback(appId,Constants.CallbackCommand.AddFriendAfter, JSON.toJSONString(addFriendAfterCallbackDto));
        }

        return ResponseVO.successResponse();
    }
}
