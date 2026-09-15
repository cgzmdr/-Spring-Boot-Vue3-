package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 民族自治地方名录（B-6）
 *
 * @author cz
 */
@Schema(description = "民族自治地方名录")
public record AutonomousAreaResource(
        @Schema(description = "统计概览") Summary summary,
        @Schema(description = "按级别分组的地方列表") List<LevelGroup> levels,
        @Schema(description = "按自治民族聚合（一个民族可对应多个自治地方）") List<EthnicGrouping> ethnics,
        @Schema(description = "按省级行政区聚合") List<ProvinceGrouping> provinces
) {
    @Schema(description = "统计概览")
    public record Summary(
            @Schema(description = "自治地方总数") int total,
            @Schema(description = "自治区数") int regionCount,
            @Schema(description = "自治州数") int prefectureCount,
            @Schema(description = "自治县/旗数") int countyCount,
            @Schema(description = "涉及自治民族数") int ethnicCount,
            @Schema(description = "涉及省级行政区数") int provinceCount
    ) {
    }

    @Schema(description = "按级别分组")
    public record LevelGroup(
            @Schema(description = "级别编码") String level,
            @Schema(description = "级别名称") String label,
            @Schema(description = "该级别数量") int count,
            @Schema(description = "该级别的地方列表") List<Area> areas
    ) {
    }

    @Schema(description = "一个自治地方")
    public record Area(
            @Schema(description = "官方全称") String name,
            @Schema(description = "级别") String level,
            @Schema(description = "级别名称") String levelLabel,
            @Schema(description = "冠名的自治民族") List<String> ethnicGroups,
            @Schema(description = "所属省级行政区") String province,
            @Schema(description = "成立年份（可能为空）") Integer establishedYear,
            @Schema(description = "行政中心 / 首府（可能为空）") String seat
    ) {
    }

    @Schema(description = "按自治民族聚合")
    public record EthnicGrouping(
            @Schema(description = "民族名称") String ethnic,
            @Schema(description = "该民族冠名的自治地方数") int count,
            @Schema(description = "该民族在内容库中的名称（若库中有此民族）") String matchedName,
            @Schema(description = "该民族在内容库中的 URL 标识") String matchedSlug,
            @Schema(description = "该民族主题色") String themeColor,
            @Schema(description = "该民族冠名的自治地方") List<Area> areas
    ) {
    }

    @Schema(description = "按省级行政区聚合")
    public record ProvinceGrouping(
            @Schema(description = "省级行政区") String province,
            @Schema(description = "下辖自治地方数") int count,
            @Schema(description = "下级自治地方（不含自治区自身）") List<Area> areas
    ) {
    }
}
