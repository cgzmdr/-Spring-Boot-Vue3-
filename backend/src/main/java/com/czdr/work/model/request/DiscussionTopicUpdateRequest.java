package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 编辑帖子请求
 */
@Schema(description = "编辑帖子请求")
public record DiscussionTopicUpdateRequest(
        @Schema(description = "标题") String title,
        @Schema(description = "正文") String content,
        @Schema(description = "配图 URL 列表") List<String> images,
        @Schema(description = "正文语言") String lang
) {
}
