package com.cj.codec.pack;

import lombok.Data;

@Data
public class LoginPack {
    private String userId;
    private Integer appId;
    private Integer clientType;
}
