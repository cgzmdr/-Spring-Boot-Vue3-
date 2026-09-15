package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 讨论区板块（C 端）
 */
@Schema(description = "讨论区板块")
public record DiscussionBoardResource(
        @Schema(description = "板块 ID") String id,
        @Schema(description = "板块标识") String slug,
        @Schema(description = "名称") String name,
        @Schema(description = "英文名称") String nameEn,
        @Schema(description = "简介") String description,
        @Schema(description = "图标标识") String icon,
        @Schema(description = "主题色") String themeColor,
        @Schema(description = "主题数") Integer topicCount
) {
}
