package com.czdr.work.model.resource;

import com.czdr.work.model.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 民族详情（完整对象）
 */
@Schema(description = "民族详情（完整对象）")
public record EthnicInfoDetailedResource(
    @Schema(description = "民族 ID") String id,
    @Schema(description = "URL 标识") String slug,
    @Schema(description = "民族名") String name,
    @Schema(description = "民族英文名") String nameEn,
    @Schema(description = "民族语自称") String selfName,
    @Schema(description = "拼音") String pinyin,
    @Schema(description = "人口") long population,
    @Schema(description = "语系") String languageFamily,
    @Schema(description = "主要聚居地") String[] region,
    @Schema(description = "语言") String[] languages,
    @Schema(description = "文字") String[] scripts,
    @Schema(description = "宗教") String[] religion,
    @Schema(description = "一句话简介") String summary,
    @Schema(description = "英文一句话简介") String summaryEn,
    @Schema(description = "详细介绍") String description,
    @Schema(description = "英文详细介绍") String descriptionEn,
    @Schema(description = "封面图") String coverImage,
    @Schema(description = "主题色") String themeColor,
    @Schema(description = "标签") String[] tags,
    @Schema(description = "内容状态（draft/pending/published/offline）") String status,
    @Schema(description = "排序号") Integer orderNum,
    @Schema(description = "创建人 ID") String createdBy,
    @Schema(description = "更新人 ID") String updatedBy,
    @Schema(description = "创建时间") LocalDateTime createdAt,
    @Schema(description = "更新时间") LocalDateTime updatedAt,
    @Schema(description = "民族习俗列表") List<EthnicCustom> customs,
    @Schema(description = "聚居地列表") List<EthnicLocation> locations,
    @Schema(description = "民族美食列表") List<Food> foods,
    @Schema(description = "民族节日列表") List<Festival> festivals,
    @Schema(description = "民族艺术列表") List<Art> arts
) {
}
