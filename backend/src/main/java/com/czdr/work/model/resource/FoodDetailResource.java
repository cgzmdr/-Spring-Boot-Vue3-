package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族美食详情（C 端公开）
 */
@Schema(description = "民族美食详情")
public record FoodDetailResource(
        @Schema(description = "美食 ID") String id,
        @Schema(description = "所属民族 ID") String ethnicGroupId,
        @Schema(description = "所属民族名称") String ethnicGroupName,
        @Schema(description = "美食名称") String name,
        @Schema(description = "美食名称（英文）") String nameEn,
        @Schema(description = "美食简介") String description,
        @Schema(description = "英文正文（方向 C-3；为空时前端回退显示 description）") String descriptionEn,
        @Schema(description = "英文正文来源：machine/reviewed/manual") String descriptionEnSource,
        @Schema(description = "发展沿革") String origin,
        @Schema(description = "图片") String image,
        @Schema(description = "排序") Integer orderNum
) {
}
