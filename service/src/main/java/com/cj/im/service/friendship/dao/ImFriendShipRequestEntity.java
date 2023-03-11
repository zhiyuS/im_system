package com.cj.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("im_friendship_request")
public class ImFriendShipRequestEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer appId;

    private String fromId;

    private String toId;
    /** 备注*/
//    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String remark;

    //是否已读 1已读
    private Integer readStatus;

    /** 好友来源*/
//    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String addSource;

    //    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String addWording;

    //审批状态 1同意 2拒绝
    private Integer approveStatus;

    //    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long createTime;

    private Long updateTime;

    /** 序列号*/
    private Long sequence;




}

