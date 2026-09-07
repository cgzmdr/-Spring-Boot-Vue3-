package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容维度统计（按内容类型与状态）
 *
 * @author cz
 */
@Schema(description = "内容维度统计（按内容类型与状态）")
public record ContentStatsResource(
        @Schema(description = "内容类型（如 ethnic/festival/art/topic）") String type,
        @Schema(description = "内容总数") long total,
        @Schema(description = "已发布数量") long published,
        @Schema(description = "草稿数量") long draft,
        @Schema(description = "待审核数量") long pending,
        @Schema(description = "已下线数量") long offline
) {
}
