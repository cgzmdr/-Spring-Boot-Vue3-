package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 人物专栏（B-7）：名录与筛选条件
 *
 * @author cz
 */
@Schema(description = "人物专栏名录")
public record PersonDirectoryResource(
        @Schema(description = "统计概览") Summary summary,
        @Schema(description = "可选筛选条件（含计数）") Filters filters,
        @Schema(description = "人物列表") List<PersonItem> persons,
        @Schema(description = "结果总数（筛选后）") int total
) {
    @Schema(description = "人物专栏统计概览")
    public record Summary(
            @Schema(description = "人物总数（按姓名去重）") int personCount,
            @Schema(description = "代表性传承人数") int inheritorCount,
            @Schema(description = "历史文化名家数") int masterCount,
            @Schema(description = "涉及民族数") int ethnicCount,
            @Schema(description = "关联非遗项目数") int projectCount
    ) {
    }

    @Schema(description = "筛选条件")
    public record Filters(
            @Schema(description = "按领域") List<Option> domains,
            @Schema(description = "按民族") List<Option> ethnics,
            @Schema(description = "按角色类型") List<Option> roles
    ) {
    }

    @Schema(description = "一个筛选项")
    public record Option(
            @Schema(description = "选项值") String value,
            @Schema(description = "显示名") String label,
            @Schema(description = "数量") int count
    ) {
    }

    @Schema(description = "人物列表项")
    public record PersonItem(
            @Schema(description = "姓名") String name,
            @Schema(description = "所属民族") String ethnicGroupName,
            @Schema(description = "角色类型：inheritor / master") String roleType,
            @Schema(description = "角色显示名") String roleLabel,
            @Schema(description = "领域") String domain,
            @Schema(description = "生卒年（仅部分名家有值）") String lifespan,
            @Schema(description = "简介（可能为空）") String bio,
            @Schema(description = "关联的非遗项目") List<ProjectRef> projects,
            @Schema(description = "其中最高非遗级别：world / national") String topLevel
    ) {
    }

    @Schema(description = "关联的非遗项目引用")
    public record ProjectRef(
            @Schema(description = "项目 ID") String id,
            @Schema(description = "项目名称") String name,
            @Schema(description = "非遗级别") String intangibleHeritage,
            @Schema(description = "所属民族") String ethnicGroupName,
            @Schema(description = "详情页路径") String detailPath
    ) {
    }
}
