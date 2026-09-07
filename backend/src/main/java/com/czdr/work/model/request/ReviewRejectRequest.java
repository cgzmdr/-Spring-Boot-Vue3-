package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审核驳回请求
 *
 * @author cz
 */
@Schema(description = "审核驳回请求")
public record ReviewRejectRequest(
        @Schema(description = "驳回原因")
        String reason
) {
}
