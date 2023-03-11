package com.cj.im.common.model;

import lombok.Data;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class SyncReq extends RequestBase {

    //客户端最大seq
    private Long lastSequence;
    //一次拉取多少
    private Integer maxLimit;

}
