package com.cj.codec.pack.group;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 解散群通知报文
 **/
@Data
public class DestroyGroupPack {

    private String groupId;

    private Long sequence;

}
