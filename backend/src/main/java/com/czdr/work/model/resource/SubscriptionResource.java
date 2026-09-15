package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 我的订阅项
 */
@Schema(description = "订阅项")
public record SubscriptionResource(
        @Schema(description = "订阅 ID") String id,
        @Schema(description = "目标类型：topic / board / user") String targetType,
        @Schema(description = "目标 ID") String targetId,
        @Schema(description = "目标名称（帖子标题 / 板块名 / 用户昵称）") String targetName,
        @Schema(description = "目标封面（可选）") String cover,
        @Schema(description = "可跳转路径") String path,
        @Schema(description = "通知强度：all / mention / off") String level,
        @Schema(description = "订阅时间") String createdAt
) {
}
