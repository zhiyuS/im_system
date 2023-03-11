package com.cj.im.service.group.model.resp;

import com.cj.im.service.group.dao.ImGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class GetJoinedGroupResp {

    private Integer totalCount;

    private List<ImGroupEntity> groupList;

}
