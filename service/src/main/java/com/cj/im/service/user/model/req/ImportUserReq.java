package com.cj.im.service.user.model.req;

import com.cj.im.common.ResponseVO;
import com.cj.im.common.model.RequestBase;
import com.cj.im.service.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

@Data
public class ImportUserReq extends RequestBase {
    public List<ImUserDataEntity> userData;

}
