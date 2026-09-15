package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 节日信息（分页列表返回的简略对象）
 */
@Schema(description = "节日信息（分页列表返回的简略对象）")
public record FestivalQueryInfoResource(
        @Schema(description = "节日 ID") String id,
        @Schema(description = "节日名") String name,
        @Schema(description = "节日英文名") String nameEn,
        @Schema(description = "节日类型（traditional/religious/agricultural）") String type,
        @Schema(description = "所属民族名称") String ethnicGroupName,
        @Schema(description = "公历日期") String solarDate,
        @Schema(description = "农历日期") String lunarDate,
        @Schema(description = "起源") String origin,
        @Schema(description = "发展沿革（详细）") String description,
        @Schema(description = "英文正文（方向 C-3；为空时前端回退显示 description）") String descriptionEn,
        @Schema(description = "英文正文来源：machine/reviewed/manual") String descriptionEnSource,
        @Schema(description = "习俗活动") String[] customs,
        @Schema(description = "图集") String[] images,
        @Schema(description = "封面图") String coverImage
) {
}
