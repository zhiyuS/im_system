package com.cj.im.common.model;

import lombok.Data;

@Data
public class RequestBase {
    public Integer appId;
    private String operater;
    private Integer clientType;
    private String imei;
}
