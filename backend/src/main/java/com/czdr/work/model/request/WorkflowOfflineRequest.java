package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容管理员审查后「暂时下线」请求。
 *
 * @author cz
 */
@Schema(description = "内容审查发现问题、暂时下线请求")
public record WorkflowOfflineRequest(
        @Schema(description = "审查意见 / 下线原因（必填，会流转给内容编辑）", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
}
