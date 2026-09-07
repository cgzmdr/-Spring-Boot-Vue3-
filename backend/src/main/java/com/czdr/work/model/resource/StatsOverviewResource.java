package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台统计总览
 *
 * @author cz
 */
@Schema(description = "后台统计总览")
public record StatsOverviewResource(
        @Schema(description = "用户总数") long userCount,
        @Schema(description = "民族总数") long ethnicCount,
        @Schema(description = "节日总数") long festivalCount,
        @Schema(description = "艺术总数") long artCount,
        @Schema(description = "专题总数") long topicCount,
        @Schema(description = "待审核内容数") long pendingReviewCount
) {
}
