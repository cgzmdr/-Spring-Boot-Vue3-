package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 发帖请求
 */
@Schema(description = "发帖请求")
public record DiscussionTopicCreateRequest(
        @Schema(description = "板块 ID") UUID boardId,
        @Schema(description = "标题") String title,
        @Schema(description = "正文（纯文本，保留换行）") String content,
        @Schema(description = "配图 URL 列表") List<String> images,
        @Schema(description = "正文语言（zh/en/...）") String lang,
        @Schema(description = "关联内容类型：ethnic/festival/art/food/topic") String linkedType,
        @Schema(description = "关联内容 ID") UUID linkedId
) {
}
