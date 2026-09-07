package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 点赞结果
 *
 * @author cz
 */
@Schema(description = "点赞结果")
public record LikeResource(
        @Schema(description = "是否已点赞") boolean liked,
        @Schema(description = "点赞数") long likeCount
) {
}
