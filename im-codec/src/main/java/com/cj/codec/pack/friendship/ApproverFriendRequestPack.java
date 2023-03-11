package com.cj.codec.pack.friendship;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 审批好友申请通知报文
 **/
@Data
public class ApproverFriendRequestPack {

    private Long id;

    //1同意 2拒绝
    private Integer status;

    private Long sequence;
}
