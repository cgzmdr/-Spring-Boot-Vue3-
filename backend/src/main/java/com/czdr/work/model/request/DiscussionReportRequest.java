package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * 举报请求
 */
@Schema(description = "举报请求")
public record DiscussionReportRequest(
        @Schema(description = "举报对象类型：discussion_topic / discussion_post") String targetType,
        @Schema(description = "举报对象 ID") UUID targetId,
        @Schema(description = "原因：spam/abuse/porn/political/copyright/other") String reason,
        @Schema(description = "补充说明") String detail
) {
}
