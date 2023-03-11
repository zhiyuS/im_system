package com.cj.codec.pack.friendship;

import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description: 删除好友分组成员通知报文
 **/
@Data
public class DeleteFriendGroupMemberPack {

    public String fromId;

    private String groupName;

    private List<String> toIds;

    /** 序列号*/
    private Long sequence;
}
