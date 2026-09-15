package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 民族人口统计（第七次全国人口普查口径）
 * <p>全部字段由 {@code ethnic_group.population}（2020 年七普）按不同维度聚合而来，
 * 供首页 / 民族频道的轻量可视化使用（PRD 6.2 数据可视化）。</p>
 *
 * @author cz
 */
@Schema(description = "民族人口统计（七普口径）")
public record EthnicPopulationStatsResource(
        @Schema(description = "统计口径年份") String censusYear,
        @Schema(description = "已发布民族数量") int totalGroups,
        @Schema(description = "已发布民族人口合计") long totalPopulation,
        @Schema(description = "人口最多民族的名称") String largestGroupName,
        @Schema(description = "人口最多民族的人口") long largestGroupPopulation,
        @Schema(description = "人口规模区间分档分布（降序）") List<Bucket> buckets,
        @Schema(description = "人口前 N 名民族（降序）") List<Item> topGroups,
        @Schema(description = "按语系聚合（降序）") List<Item> languageFamilies,
        @Schema(description = "按聚居地域聚合（降序，一个民族可计入多个地域）") List<Item> regions
) {
    @Schema(description = "一项聚合结果")
    public record Item(
            @Schema(description = "分组名称") String name,
            @Schema(description = "该分组下的民族数量") int groupCount,
            @Schema(description = "该分组下的民族人口合计") long population
    ) {
    }

    @Schema(description = "人口规模分档")
    public record Bucket(
            @Schema(description = "分档标签，如「1000 万以上」") String label,
            @Schema(description = "该档民族数量") int groupCount,
            @Schema(description = "该档民族人口合计") long population
    ) {
    }
}
