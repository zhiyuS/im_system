package com.cj.im.service.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cj.codec.pack.user.UserModifyPack;
import com.cj.im.common.ResponseVO;
import com.cj.im.common.config.AppConfig;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.DelFlagEnum;
import com.cj.im.common.enums.UserErrorCode;
import com.cj.im.common.enums.command.UserEventCommand;
import com.cj.im.common.exception.ApplicationException;
import com.cj.im.service.user.dao.ImUserDataEntity;
import com.cj.im.service.user.dao.mapper.ImUserDataMapper;
import com.cj.im.service.user.model.req.*;
import com.cj.im.service.user.model.resp.GetUserInfoResp;
import com.cj.im.service.user.model.resp.ImportUserResp;
import com.cj.im.service.user.service.UserService;
import com.cj.im.service.utils.CallBackService;
import com.cj.im.service.utils.MessageProduce;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private ImUserDataMapper userDataMapper;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallBackService callBackService;

    @Autowired
    private MessageProduce messageProducer;

    /**
     * 导入用户
     * @param req
     * @return
     */
    @Override
    public ResponseVO importUser(ImportUserReq req) {

        if(req.getUserData().size()>100){

        }
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();
        req.getUserData().forEach(user->{
            try {
                user.setAppId(req.getAppId());
                System.out.println(user);
                int insert = userDataMapper.insert(user);
                if(insert == 1){
                    successId.add(user.getUserId());
                }
            } catch (Exception e) {
                errorId.add(user.getUserId());
                e.printStackTrace();
            }
        });
        ImportUserResp importUserResp = new ImportUserResp();
        importUserResp.setSuccessId(successId);
        importUserResp.setErrorId(errorId);
        return ResponseVO.successResponse(importUserResp);
    }

    /**
     * 获取用户信息
     * @param req
     * @return
     */
    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        QueryWrapper<ImUserDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.in("user_id",req.getUserIds());
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        List<ImUserDataEntity> userDataEntities = userDataMapper.selectList(queryWrapper);
        HashMap<String, ImUserDataEntity> map = new HashMap<>();

        for (ImUserDataEntity data:
                userDataEntities) {
            map.put(data.getUserId(),data);
        }

        List<String> failUser = new ArrayList<>();
        for (String uid:
                req.getUserIds()) {
            if(!map.containsKey(uid)){
                failUser.add(uid);
            }
        }

        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setUserDataItem(userDataEntities);
        resp.setFailUser(failUser);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        QueryWrapper<ImUserDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        queryWrapper.eq("app_id",appId);

        ImUserDataEntity imUserDataEntity = userDataMapper.selectOne(queryWrapper);
        if(ObjectUtil.isNull(imUserDataEntity)){
            return ResponseVO.errorResponse();
        }
        return ResponseVO.successResponse(imUserDataEntity);
    }

    /**
     * 删除用户
     * @param req
     * @return
     */
    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {
        ImUserDataEntity entity = new ImUserDataEntity();
        entity.setDelFlag(DelFlagEnum.DELETE.getCode());

        List<String> errorId = new ArrayList();
        List<String> successId = new ArrayList();

        for (String userId:
                req.getUserId()) {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("app_id",req.getAppId());
            wrapper.eq("user_id",userId);
            wrapper.eq("del_flag",DelFlagEnum.NORMAL.getCode());
            int update = 0;

            try {
                update =  userDataMapper.update(entity, wrapper);
                if(update > 0){
                    successId.add(userId);
                }else{
                    errorId.add(userId);
                }
            }catch (Exception e){
                errorId.add(userId);
            }
        }

        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }

    /**
     * 修改用户
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        QueryWrapper query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("user_id",req.getUserId());
        query.eq("del_flag",DelFlagEnum.NORMAL.getCode());
        ImUserDataEntity user = userDataMapper.selectOne(query);
        if(user == null){
            throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }

        ImUserDataEntity update = new ImUserDataEntity();
        BeanUtils.copyProperties(req,update);

        update.setAppId(null);
        update.setUserId(null);
        int update1 = userDataMapper.update(update, query);

        //修改成功
        if(update1 == 1){

            UserModifyPack pack = new UserModifyPack();
            BeanUtils.copyProperties(req,pack);
            messageProducer.sendToUser(req.getUserId(),req.getClientType(),req.getImei(),
                    UserEventCommand.USER_MODIFY,pack,req.getAppId());

            if(appConfig.isModifyUserAfterCallback()){
                callBackService.callback(req.getAppId(), Constants.CallbackCommand.ModifyUserAfter, JSON.toJSONString(req));
            }
            return ResponseVO.successResponse();
        }
        throw new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }

    /**
     * 登录
     * @param req
     * @return
     */
    @Override
    public ResponseVO login(LoginReq req) {
        return ResponseVO.successResponse();
    }

    /**
     * 获取用户Sequence
     * @param req
     * @return
     */
    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        return null;
    }
}
