package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 传统体育名录（B-5）
 *
 * @author cz
 */
@Schema(description = "传统体育项目名录")
public record TraditionalSportResource(
        @Schema(description = "统计概览") Summary summary,
        @Schema(description = "项目列表") List<Sport> sports
) {
    @Schema(description = "统计概览")
    public record Summary(
            @Schema(description = "项目总数") int total,
            @Schema(description = "类别数") int categoryCount,
            @Schema(description = "涉及民族数") int ethnicCount,
            @Schema(description = "含子项的项目数") int withSubEvents,
            @Schema(description = "有关联非遗的项目数") int withHeritage
    ) {
    }

    @Schema(description = "一个传统体育项目")
    public record Sport(
            @Schema(description = "项目名称") String name,
            @Schema(description = "类别编码") String category,
            @Schema(description = "类别名称") String categoryLabel,
            @Schema(description = "起源与主要流行的民族") List<String> ethnicOrigins,
            @Schema(description = "描述") String description,
            @Schema(description = "主要器材") String equipment,
            @Schema(description = "场地规格") String venue,
            @Schema(description = "参赛人数说明") String teamSize,
            @Schema(description = "首次成为运动会竞赛项目的年份") Integer firstEventYear,
            @Schema(description = "子项名称（多子项项目）") List<String> subEvents,
            @Schema(description = "关联的国家级非遗项目名") String heritageLink,
            @Schema(description = "起源民族中能在内容库匹配到的（用于跳转）") List<EthnicRef> matchedEthnics
    ) {
    }

    @Schema(description = "匹配到的民族引用")
    public record EthnicRef(
            @Schema(description = "民族 ID") String id,
            @Schema(description = "民族名称") String name,
            @Schema(description = "URL 标识") String slug,
            @Schema(description = "主题色") String themeColor
    ) {
    }
}
