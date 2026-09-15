package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 举报处置 / 用户处置请求（后台）
 */
@Schema(description = "举报处置或用户处置请求")
public record DiscussionHandleRequest(
        @Schema(description = "举报结论：accepted 举报成立 / rejected 举报不成立") String status,
        @Schema(description = "处理说明（会通知举报人）") String note,
        @Schema(description = "同时对被举报内容执行的处置：none / hide / delete") String contentAction,
        @Schema(description = "对作者禁言天数（0 或空表示不禁言）") Integer muteDays
) {
}
