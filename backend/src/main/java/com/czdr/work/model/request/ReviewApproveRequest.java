package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审核/审查「通过」请求：必填审批意见或审查意见。
 *
 * @author cz
 */
@Schema(description = "审核/审查通过请求")
public record ReviewApproveRequest(
        @Schema(description = "审批/审查意见（会留档并展示给内容编辑）", requiredMode = Schema.RequiredMode.REQUIRED)
        String opinion
) {
}
