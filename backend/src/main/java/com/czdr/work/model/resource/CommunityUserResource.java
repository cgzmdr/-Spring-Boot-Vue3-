package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 社区用户资料（用户页 / 关注按钮）
 */
@Schema(description = "社区用户资料")
public record CommunityUserResource(
        @Schema(description = "用户 ID") String id,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "头像") String avatar,
        @Schema(description = "个人简介") String bio,
        @Schema(description = "所在地/语言偏好") String locale,
        @Schema(description = "信任等级") Integer trustLevel,
        @Schema(description = "加入时间") String joinedAt,
        @Schema(description = "关注数") long followingCount,
        @Schema(description = "粉丝数") long followerCount,
        @Schema(description = "发帖数") long topicCount,
        @Schema(description = "我是否已关注") boolean followed,
        @Schema(description = "是否互相关注（互关后可互发私信）") boolean mutual,
        @Schema(description = "是否为本人") boolean self
) {
}
