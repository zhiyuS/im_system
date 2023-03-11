package com.cj.codec.pack.friendship;

import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description: 好友分组添加成员通知包
 **/
@Data
public class AddFriendGroupMemberPack {

    public String fromId;

    private String groupName;

    private List<String> toIds;

    /** 序列号*/
    private Long sequence;
}
