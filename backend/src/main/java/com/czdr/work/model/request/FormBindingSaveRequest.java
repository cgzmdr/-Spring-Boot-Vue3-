package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 保存（新建/更新）Camunda Form 定义请求。
 *
 * @author cz
 */
@Schema(description = "保存 Camunda Form 定义请求")
public record FormBindingSaveRequest(
        @Schema(description = "表单 formId，与 BPMN 中 userTask 的 formId 一致") String formId,
        @Schema(description = "表单名称") String name,
        @Schema(description = "用途：approval 审批 / inspection 审查 / revision 修改") String purpose,
        @Schema(description = "业务类型，* 表示通用") String entryType,
        @Schema(description = "form-js schema JSON 原文") String schemaJson,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark
) {
}
