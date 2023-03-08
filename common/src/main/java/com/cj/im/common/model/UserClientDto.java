package com.cj.im.common.model;

import lombok.Data;

@Data
public class UserClientDto {
    private String userId;
    private Integer appId;
    private Integer clientType;
    private String imei;

}
