package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 互动统计
 *
 * @author cz
 */
@Schema(description = "互动统计")
public record StatsResource(
        @Schema(description = "点赞数") long likeCount,
        @Schema(description = "收藏数") long favoriteCount,
        @Schema(description = "浏览量") long viewCount
) {
}
