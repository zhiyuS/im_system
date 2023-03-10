package com.cj.im.service.friendship.model.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.cj.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class SaveFriendShipReq extends RequestBase {

    @NotEmpty(message = "fromId不能为空")
    private String fromId;

    @NotEmpty(message = "friendDto不能为空")
    private FriendDto friendDto;


}
