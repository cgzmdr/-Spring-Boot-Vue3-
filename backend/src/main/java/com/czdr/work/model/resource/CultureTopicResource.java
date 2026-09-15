package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文化专题聚合（服饰 / 民居建筑）
 *
 * <p>设计意图：库中同一主题的内容分散在两张表 ——
 * {@code ethnic_custom}（风俗习惯，含服饰 / 居住 / 建筑等分类，讲**文化特征**）
 * 与 {@code art}（传统艺术与非遗项目，讲**技艺与名录级别**，且有专属配图）。
 * 本模型把两者按民族合流为一个专题条目，使「服饰」「民居」这类维度
 * 能作为独立专题呈现，而不必新增内容表或重复录入。</p>
 *
 * @author cz
 */
@Schema(description = "文化专题聚合（服饰 / 民居建筑）")
public record CultureTopicResource(
        @Schema(description = "专题标识：costume / dwelling") String topic,
        @Schema(description = "专题中文名") String title,
        @Schema(description = "专题英文名") String titleEn,
        @Schema(description = "专题简介") String intro,
        @Schema(description = "统计概览") Summary summary,
        @Schema(description = "分类维度（如服饰的工艺技法、民居的建筑形制）") List<Category> categories,
        @Schema(description = "按民族聚合的专题条目") List<Entry> entries
) {
    @Schema(description = "专题统计概览")
    public record Summary(
            @Schema(description = "涉及民族数") int groupCount,
            @Schema(description = "条目总数") int entryCount,
            @Schema(description = "其中属于非遗名录的条目数") int heritageCount,
            @Schema(description = "有配图的条目数") int withImage
    ) {
    }

    @Schema(description = "一个分类维度")
    public record Category(
            @Schema(description = "分类编码") String code,
            @Schema(description = "分类名称") String label,
            @Schema(description = "条目数") int count
    ) {
    }

    @Schema(description = "按民族聚合的专题条目")
    public record Entry(
            @Schema(description = "民族 ID") String ethnicGroupId,
            @Schema(description = "民族名称") String ethnicGroupName,
            @Schema(description = "民族主题色") String themeColor,
            @Schema(description = "民族所在地域（取 region 首项）") String region,
            @Schema(description = "配图（优先取该民族非遗项目封面，其次民族封面）") String coverImage,
            @Schema(description = "该民族在此专题下的内容条目") List<Item> items
    ) {
    }

    @Schema(description = "专题下的单条内容")
    public record Item(
            @Schema(description = "来源：custom=风俗习惯 / art=非遗项目") String source,
            @Schema(description = "条目 ID") String id,
            @Schema(description = "标题") String title,
            @Schema(description = "所属分类编码（仅 custom 有值）") String category,
            @Schema(description = "正文摘要") String content,
            @Schema(description = "非遗级别（仅 art 有值：world / national）") String intangibleHeritage,
            @Schema(description = "配图（仅 art 有值）") String image,
            @Schema(description = "详情页路径") String detailPath
    ) {
    }
}
