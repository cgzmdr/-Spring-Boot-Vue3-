package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 艺术信息（分页列表返回的简略对象）
 */
@Schema(description = "艺术信息（分页列表返回的简略对象）")
public record ArtQueryInfoResource(
        @Schema(description = "艺术 ID") String id,
        @Schema(description = "艺术名") String name,
        @Schema(description = "艺术英文名") String nameEn,
        @Schema(description = "类别") String category,
        @Schema(description = "所属民族名称") String ethnicGroupName,
        @Schema(description = "介绍") String description,
        @Schema(description = "英文正文（方向 C-3；为空时前端回退显示 description）") String descriptionEn,
        @Schema(description = "英文正文来源：machine/reviewed/manual") String descriptionEnSource,
        @Schema(description = "发展沿革") String origin,
        @Schema(description = "非遗级别") String intangibleHeritage,
        @Schema(description = "传承人") String[] inheritors,
        @Schema(description = "封面图") String coverImage
) {
}
