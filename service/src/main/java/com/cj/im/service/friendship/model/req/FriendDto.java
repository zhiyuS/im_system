package com.cj.im.service.friendship.model.req;

import lombok.Data;

@Data
public class FriendDto {
    private String toId;

    private String remark;

    private String addSource;

    private String extra;
    private String addWording;

}
