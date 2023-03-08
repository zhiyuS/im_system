package com.cj.codec.proto;

import lombok.Data;

@Data
public class MessageHeader {
    private Integer command;
    //4字节 版本号
    private Integer version;
    //4字节 端类型
    private Integer clientType;
    /**
     * 应用ID
     */
    //4字节 appId
    private Integer appId;
    /**
     * 数据解析类型 和具体业务无关，后续根据解析类型解析data数据 0x0:Json,0x1:ProtoBuf,0x2:Xml,默认:0x0
     */
    //4字节 解析类型
    private Integer messageType = 0x0;

    //4字节 imel长度
    private Integer imeiLength;

    //4字节 包体长度
    private int bodyLen;

    //imei号
    private String imei;


}
