package com.cj.codec.pack.friendship;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 添加好友通知报文
 **/
@Data
public class AddFriendPack {
    private String fromId;

    /**
     * 备注
     */
    private String remark;
    private String toId;
    /**
     * 好友来源
     */
    private String addSource;
    /**
     * 添加好友时的描述信息（用于打招呼）
     */
    private String addWording;

    private Long sequence;
}
