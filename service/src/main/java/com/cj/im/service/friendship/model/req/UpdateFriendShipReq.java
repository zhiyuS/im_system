package com.cj.im.service.friendship.model.req;


import com.cj.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class UpdateFriendShipReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotNull(message = "friendDto不能为空")
    private FriendDto friendDto;

}
