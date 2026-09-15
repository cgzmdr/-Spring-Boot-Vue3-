package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 非物质文化遗产名录（统计概览）
 * <p>基于 {@code art} 表 165 项非遗项目聚合，用于名录页顶部概览与筛选条件。</p>
 *
 * @author cz
 */
@Schema(description = "非遗名录统计概览")
public record HeritageDirectoryStatsResource(
        @Schema(description = "非遗项目总数") int total,
        @Schema(description = "有传承人记录的项目数") int withInheritor,
        @Schema(description = "涉及民族数量") int ethnicCount,
        @Schema(description = "按级别分组统计") List<Group> levels,
        @Schema(description = "按类别分组统计") List<Group> categories,
        @Schema(description = "传承人最多的项目（样本，按人数降序）") List<InheritorSpotlight> spotlight
) {
    @Schema(description = "一个分组统计项")
    public record Group(
            @Schema(description = "分组编码（如 world / craft）") String code,
            @Schema(description = "分组名称") String label,
            @Schema(description = "项目数量") int count
    ) {
    }

    @Schema(description = "传承人聚焦")
    public record InheritorSpotlight(
            @Schema(description = "项目 ID") String id,
            @Schema(description = "项目名称") String name,
            @Schema(description = "非遗级别") String intangibleHeritage,
            @Schema(description = "所属民族") String ethnicGroupName,
            @Schema(description = "传承人") List<String> inheritors
    ) {
    }
}
