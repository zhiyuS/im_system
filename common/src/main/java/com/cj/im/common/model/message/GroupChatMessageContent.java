package com.cj.im.common.model.message;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class GroupChatMessageContent extends MessageContent {

    private String groupId;

    private List<String> memberId;

}
