package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 回复（楼层）请求
 */
@Schema(description = "回复请求")
public record DiscussionPostCreateRequest(
        @Schema(description = "正文") String content,
        @Schema(description = "配图 URL 列表") List<String> images,
        @Schema(description = "语言") String lang,
        @Schema(description = "引用的楼层 ID") UUID quotePostId,
        @Schema(description = "父楼层 ID（楼中楼，暂不使用）") UUID parentId
) {
}
