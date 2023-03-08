package com.cj.im.common.model;

import lombok.Data;

@Data
public class UserSession {
    private String userId;
    private Integer appId;
    /**
     * 端的表示
     */
    private Integer clientType;
    /**
     * 版本
     */
    private Integer version;

    /**
     * 连接状态 1：上线 2：离线
     */
    private Integer connectStatus;
    /**
     * brokerId
     */
    private Integer brokerId;
    /**
     * brokerHost
     */
    private String brokerHost;
    /**
     * imei
     */
    private String imei;

}
