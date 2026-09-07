package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 搜索聚合结果
 *
 * @author cz
 */
@Schema(description = "搜索聚合结果")
public record SearchResultResource(
        @Schema(description = "民族搜索结果分组") SearchGroupResource ethnic,
        @Schema(description = "节日搜索结果分组") SearchGroupResource festival,
        @Schema(description = "艺术搜索结果分组") SearchGroupResource art
) {
}
