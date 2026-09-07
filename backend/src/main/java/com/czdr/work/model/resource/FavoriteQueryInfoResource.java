package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 我的收藏
 *
 * @author cz
 */
@Schema(description = "我的收藏")
public record FavoriteQueryInfoResource(
        @Schema(description = "收藏记录 ID") String id,
        @Schema(description = "收藏内容类型（ethnic/festival/art/topic）") String entryType,
        @Schema(description = "收藏内容 ID") String entryId,
        @Schema(description = "收藏内容名称") String entryName,
        @Schema(description = "封面图") String coverImage,
        @Schema(description = "收藏时间") LocalDateTime createdAt
) {
}
