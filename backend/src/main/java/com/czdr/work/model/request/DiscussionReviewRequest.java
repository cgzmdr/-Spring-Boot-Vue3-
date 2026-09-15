package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审核处置请求（后台）
 */
@Schema(description = "内容审核处置请求")
public record DiscussionReviewRequest(
        @Schema(description = "目标类型：discussion_topic / discussion_post") String targetType,
        @Schema(description = "目标 ID") String targetId,
        @Schema(description = "结论：approved 通过 / rejected 驳回") String status,
        @Schema(description = "驳回或处置理由（会通知作者）") String reason
) {
}
