package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 提交内容审批请求。
 *
 * @author cz
 */
@Schema(description = "提交内容审批请求")
public record WorkflowSubmitRequest(
        @Schema(description = "提交说明（可空，会作为一条「提交审批」意见留痕）")
        String note
) {
}
