package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族简略模型（全家福互动墙）
 *
 * @author cz
 */
@Schema(description = "民族简略模型（全家福互动墙）")
public record EthnicBriefResource(
        @Schema(description = "民族 ID") String id,
        @Schema(description = "民族名称") String name,
        @Schema(description = "主题色") String themeColor,
        @Schema(description = "封面图") String coverImage
) {
}
