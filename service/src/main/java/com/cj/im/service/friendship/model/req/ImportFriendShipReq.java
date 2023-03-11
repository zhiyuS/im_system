package com.cj.im.service.friendship.model.req;

import com.cj.im.common.enums.FriendShipStatusEnum;
import com.cj.im.common.model.RequestBase;
import com.cj.im.service.friendship.dao.ImFriendShipEntity;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ImportFriendShipReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;
    public List<ImportFriendDto> friendData;

    @Data
    public static class ImportFriendDto{

        private String toId;

        private String remark;

        private String addSource;

        private Integer status = FriendShipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode();

        private Integer black = FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode();
    }

}
