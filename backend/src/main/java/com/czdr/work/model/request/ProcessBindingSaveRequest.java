package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 保存（新建/更新）流程定义或表单定义请求。
 *
 * @author cz
 */
@Schema(description = "保存 BPMN 流程定义请求")
public record ProcessBindingSaveRequest(
        @Schema(description = "流程标识，如 ethnic-content-review") String processKey,
        @Schema(description = "流程名称") String name,
        @Schema(description = "业务类型：ethnic / festival / art / topic / *") String entryType,
        @Schema(description = "BPMN XML 原文") String bpmnXml,
        @Schema(description = "是否启用；启用时会停用同 entryType 下的其他版本") Boolean enabled,
        @Schema(description = "备注") String remark
) {
}
