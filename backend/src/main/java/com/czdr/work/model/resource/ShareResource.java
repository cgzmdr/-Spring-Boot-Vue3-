package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分享结果
 *
 * @author cz
 */
@Schema(description = "分享结果")
public record ShareResource(
        @Schema(description = "分享链接") String shareUrl,
        @Schema(description = "海报图片链接") String posterUrl
) {
}
