package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 站内通知
 */
@Schema(description = "站内通知")
public record NotificationResource(
        @Schema(description = "通知 ID") String id,
        @Schema(description = "类型：topic_reply/post_reply/like/favorite/review_result/report_result/system") String type,
        @Schema(description = "触发者昵称") String actorName,
        @Schema(description = "触发者头像") String actorAvatar,
        @Schema(description = "标题") String title,
        @Schema(description = "内容") String content,
        @Schema(description = "跳转类型") String targetType,
        @Schema(description = "跳转目标 ID") String targetId,
        @Schema(description = "自定义跳转链接（站内公告用，如 /discussion）") String link,
        @Schema(description = "是否已读") Boolean read,
        @Schema(description = "创建时间") String createdAt
) {
}
