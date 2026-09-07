package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族风俗习惯详情（C 端公开）
 */
@Schema(description = "民族风俗习惯详情")
public record EthnicCustomDetailResource(
        @Schema(description = "风俗 ID") String id,
        @Schema(description = "所属民族 ID") String ethnicGroupId,
        @Schema(description = "所属民族名称") String ethnicGroupName,
        @Schema(description = "风俗分类") String category,
        @Schema(description = "风俗名称") String title,
        @Schema(description = "风俗详细内容") String content,
        @Schema(description = "图片") String image,
        @Schema(description = "排序") Integer orderNum
) {
}
