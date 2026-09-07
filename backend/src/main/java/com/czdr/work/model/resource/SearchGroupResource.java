package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 搜索分组结果（按内容类型分组）
 *
 * @author cz
 */
@Schema(description = "搜索分组结果（按内容类型分组）")
public record SearchGroupResource(
        @Schema(description = "该类型搜索结果总数") long total,
        @Schema(description = "搜索结果列表（按类型不同为民族/节日/艺术对象）") List<?> list
) {
}
