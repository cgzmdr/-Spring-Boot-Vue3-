package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 生成分享请求
 *
 * @author cz
 */
@Schema(description = "生成分享链接/海报请求")
public record ShareRequest(
        @Schema(description = "内容类型（ethnic/festival/art/topic）")
        String type,
        @Schema(description = "内容 ID")
        String id
) {
}
