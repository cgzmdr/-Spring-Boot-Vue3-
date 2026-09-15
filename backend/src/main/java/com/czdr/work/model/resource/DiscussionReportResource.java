package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 举报（后台工作台 / 用户回执）
 */
@Schema(description = "举报记录")
public record DiscussionReportResource(
        @Schema(description = "举报 ID") String id,
        @Schema(description = "被举报对象类型") String targetType,
        @Schema(description = "被举报对象 ID") String targetId,
        @Schema(description = "被举报内容摘要") String targetExcerpt,
        @Schema(description = "被举报内容作者昵称") String targetAuthor,
        @Schema(description = "被举报内容链接路径") String targetPath,
        @Schema(description = "举报人昵称") String reporter,
        @Schema(description = "举报原因") String reason,
        @Schema(description = "补充说明") String detail,
        @Schema(description = "状态：pending/accepted/rejected") String status,
        @Schema(description = "处理说明") String resultNote,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "处理时间") String handledAt
) {
}
