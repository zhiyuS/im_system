package com.cj.im.service.friendship.model.req;

import com.cj.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GetFriendReq extends RequestBase {
    @NotBlank(message = "fromId 不能为空")
    private String fromId;

    @NotBlank(message = "toId 不能为空")
    private String toId;
}
