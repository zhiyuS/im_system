/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : localhost:3306
 Source Schema         : im-core

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 08/01/2023 11:10:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_user
-- ----------------------------
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user`  (
  `user_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` bigint(20) NULL DEFAULT NULL,
  `update_time` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_conversation_set
-- ----------------------------
DROP TABLE IF EXISTS `im_conversation_set`;
CREATE TABLE `im_conversation_set`  (
  `conversation_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `conversation_type` int(10) NULL DEFAULT NULL COMMENT '0 单聊 1群聊 2机器人 3公众号',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `to_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `is_mute` int(10) NULL DEFAULT NULL COMMENT '是否免打扰 1免打扰',
  `is_top` int(10) NULL DEFAULT NULL COMMENT '是否置顶 1置顶',
  `sequence` bigint(20) NULL DEFAULT NULL COMMENT 'sequence',
  `readed_sequence` bigint(20) NULL DEFAULT NULL,
  `app_id` int(10) NOT NULL,
  PRIMARY KEY (`app_id`, `conversation_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_friendship
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship`;
CREATE TABLE `im_friendship`  (
  `app_id` int(20) NOT NULL COMMENT 'app_id',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'from_id',
  `to_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'to_id',
  `remark` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` int(10) NULL DEFAULT NULL COMMENT '状态 1正常 2删除',
  `black` int(10) NULL DEFAULT NULL COMMENT '1正常 2拉黑',
  `create_time` bigint(20) NULL DEFAULT NULL,
  `friend_sequence` bigint(20) NULL DEFAULT NULL,
  `black_sequence` bigint(20) NULL DEFAULT NULL,
  `add_source` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '来源',
  `extra` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '来源',
  PRIMARY KEY (`app_id`, `from_id`, `to_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_friendship_group
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_group`;
CREATE TABLE `im_friendship_group`  (
  `app_id` int(20) NULL DEFAULT NULL COMMENT 'app_id',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'from_id',
  `group_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `group_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `sequence` bigint(20) NULL DEFAULT NULL,
  `create_time` bigint(20) NULL DEFAULT NULL,
  `update_time` bigint(20) NULL DEFAULT NULL,
  `del_flag` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`group_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_friendship_group_member
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_group_member`;
CREATE TABLE `im_friendship_group_member`  (
  `group_id` bigint(20) NOT NULL,
  `to_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`group_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_friendship_request
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_request`;
CREATE TABLE `im_friendship_request`  (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` int(20) NULL DEFAULT NULL COMMENT 'app_id',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'from_id',
  `to_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'to_id',
  `remark` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `read_status` int(10) NULL DEFAULT NULL COMMENT '是否已读 1已读',
  `add_source` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '好友来源',
  `add_wording` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '好友验证信息',
  `approve_status` int(10) NULL DEFAULT NULL COMMENT '审批状态 1同意 2拒绝',
  `create_time` bigint(20) NULL DEFAULT NULL,
  `update_time` bigint(20) NULL DEFAULT NULL,
  `sequence` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_group
-- ----------------------------
DROP TABLE IF EXISTS `im_group`;
CREATE TABLE `im_group`  (
  `app_id` int(20) NOT NULL COMMENT 'app_id',
  `group_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'group_id',
  `owner_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '群主\r\n',
  `group_type` int(10) NULL DEFAULT NULL COMMENT '群类型 1私有群（类似微信） 2公开群(类似qq）',
  `group_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `mute` int(10) NULL DEFAULT NULL COMMENT '是否全员禁言，0 不禁言；1 全员禁言',
  `apply_join_type` int(10) NULL DEFAULT NULL COMMENT '//    申请加群选项包括如下几种：\r\n//    0 表示禁止任何人申请加入\r\n//    1 表示需要群主或管理员审批\r\n//    2 表示允许无需审批自由加入群组',
  `photo` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `max_member_count` int(20) NULL DEFAULT NULL,
  `introduction` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '群简介',
  `notification` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '群公告',
  `status` int(5) NULL DEFAULT NULL COMMENT '群状态 0正常 1解散',
  `sequence` bigint(20) NULL DEFAULT NULL,
  `create_time` bigint(20) NULL DEFAULT NULL,
  `extra` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '来源',
  `update_time` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`app_id`, `group_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_group_member
-- ----------------------------
DROP TABLE IF EXISTS `im_group_member`;
CREATE TABLE `im_group_member`  (
  `group_member_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'group_id',
  `app_id` int(10) NULL DEFAULT NULL,
  `member_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '成员id\r\n',
  `role` int(10) NULL DEFAULT NULL COMMENT '群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员',
  `speak_date` bigint(100) NULL DEFAULT NULL,
  `mute` int(10) NULL DEFAULT NULL COMMENT '是否全员禁言，0 不禁言；1 全员禁言',
  `alias` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '群昵称',
  `join_time` bigint(20) NULL DEFAULT NULL COMMENT '加入时间',
  `introduction` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '群简介',
  `notification` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '群公告',
  `leave_time` bigint(20) NULL DEFAULT NULL COMMENT '离开时间',
  `join_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '加入类型',
  `extra` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`group_member_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_group_message_history
-- ----------------------------
DROP TABLE IF EXISTS `im_group_message_history`;
CREATE TABLE `im_group_message_history`  (
  `app_id` int(20) NOT NULL COMMENT 'app_id',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'from_id',
  `group_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'group_id',
  `message_key` bigint(50) NOT NULL COMMENT 'messageBodyId',
  `create_time` bigint(20) NULL DEFAULT NULL,
  `sequence` bigint(20) NULL DEFAULT NULL,
  `message_random` int(20) NULL DEFAULT NULL,
  `message_time` bigint(20) NULL DEFAULT NULL COMMENT '来源',
  PRIMARY KEY (`app_id`, `group_id`, `message_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_message_body
-- ----------------------------
DROP TABLE IF EXISTS `im_message_body`;
CREATE TABLE `im_message_body`  (
  `app_id` int(10) NOT NULL,
  `message_key` bigint(50) NOT NULL,
  `message_body` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `security_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `message_time` bigint(20) NULL DEFAULT NULL,
  `create_time` bigint(20) NULL DEFAULT NULL,
  `extra` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`app_id`, `message_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_message_history
-- ----------------------------
DROP TABLE IF EXISTS `im_message_history`;
CREATE TABLE `im_message_history`  (
  `app_id` int(20) NOT NULL COMMENT 'app_id',
  `from_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'from_id',
  `to_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'to_id\r\n',
  `owner_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'owner_id\r\n',
  `message_key` bigint(50) NOT NULL COMMENT 'messageBodyId',
  `create_time` bigint(20) NULL DEFAULT NULL,
  `sequence` bigint(20) NULL DEFAULT NULL,
  `message_random` int(20) NULL DEFAULT NULL,
  `message_time` bigint(20) NULL DEFAULT NULL COMMENT '来源',
  PRIMARY KEY (`app_id`, `owner_id`, `message_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_user_data
-- ----------------------------
DROP TABLE IF EXISTS `im_user_data`;
CREATE TABLE `im_user_data`  (
  `user_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `app_id` int(11) NOT NULL,
  `nick_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `user_sex` int(10) NULL DEFAULT NULL,
  `birth_day` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生日',
  `location` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地址',
  `self_signature` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '个性签名',
  `friend_allow_type` int(10) NOT NULL DEFAULT 1 COMMENT '加好友验证类型（Friend_AllowType） 1无需验证 2需要验证',
  `forbidden_flag` int(10) NOT NULL DEFAULT 0 COMMENT '禁用标识 1禁用',
  `disable_add_friend` int(10) NOT NULL DEFAULT 0 COMMENT '管理员禁止用户添加加好友：0 未禁用 1 已禁用',
  `silent_flag` int(10) NOT NULL DEFAULT 0 COMMENT '禁言标识 1禁言',
  `user_type` int(10) NOT NULL DEFAULT 1 COMMENT '用户类型 1普通用户 2客服 3机器人',
  `del_flag` int(20) NOT NULL DEFAULT 0,
  `extra` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`app_id`, `user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
