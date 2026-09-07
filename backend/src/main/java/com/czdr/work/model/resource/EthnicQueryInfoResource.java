package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族列表项（分页列表返回的简略对象）
 */
@Schema(description = "民族列表项（分页列表返回的简略对象）")
public record EthnicQueryInfoResource(
        @Schema(description = "民族 ID") String id,
        @Schema(description = "民族名称") String name,
        @Schema(description = "拼音") String pinyin,
        @Schema(description = "人口") long population,
        @Schema(description = "主要聚居地") String[] region,
        @Schema(description = "语系") String languageFamily,
        @Schema(description = "一句话简介") String summary,
        @Schema(description = "封面图") String coverImage,
        @Schema(description = "主题色") String themeColor
) {
}
