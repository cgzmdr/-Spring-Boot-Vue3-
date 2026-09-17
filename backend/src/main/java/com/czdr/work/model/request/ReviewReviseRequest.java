package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容编辑「修改完成」请求：修改说明 + 是否需要再次审批。
 *
 * @author cz
 */
@Schema(description = "内容编辑修改完成请求")
public record ReviewReviseRequest(
        @Schema(description = "修改说明（针对前序审批/审查意见做了哪些修改）")
        String revisionNote,
        @Schema(description = "修改完成后是否再次提交审核员审批；false 表示直接重新上线（仅内容管理员下线场景可用）")
        Boolean needReapproval
) {
}
