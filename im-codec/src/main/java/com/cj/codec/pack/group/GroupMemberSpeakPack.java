package com.cj.codec.pack.group;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 群成员禁言通知报文
 **/
@Data
public class GroupMemberSpeakPack {

    private String groupId;

    private String memberId;

    private Long speakDate;

}
