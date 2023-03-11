package com.cj.im.service.group.model.req;

import com.cj.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class MuteGroupReq extends RequestBase {

    @NotBlank(message = "groupId不能为空")
    private String groupId;

    @NotNull(message = "mute不能为空")
    private Integer mute;

}
