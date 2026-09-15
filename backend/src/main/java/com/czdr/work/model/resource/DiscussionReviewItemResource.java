package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 待审内容条目（后台审核队列）
 */
@Schema(description = "待审内容条目")
public record DiscussionReviewItemResource(
        @Schema(description = "目标类型：discussion_topic / discussion_post") String targetType,
        @Schema(description = "目标 ID") String targetId,
        @Schema(description = "标题（帖子）或楼层号") String title,
        @Schema(description = "内容摘要") String excerpt,
        @Schema(description = "所属板块 / 帖子") String context,
        @Schema(description = "作者 ID") String authorId,
        @Schema(description = "作者昵称") String authorName,
        @Schema(description = "命中等级：block/watch") String riskLevel,
        @Schema(description = "命中词") String hitWords,
        @Schema(description = "被举报次数") Long reportCount,
        @Schema(description = "状态") String status,
        @Schema(description = "提交时间") String createdAt
) {
}
