package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 敏感词维护请求（后台）
 */
@Schema(description = "敏感词维护请求")
public record SensitiveWordRequest(
        @Schema(description = "词条") String word,
        @Schema(description = "语种：zh/en/...") String locale,
        @Schema(description = "等级：block 进待审 / watch 打标 / replace 替换") String level,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark
) {
}
