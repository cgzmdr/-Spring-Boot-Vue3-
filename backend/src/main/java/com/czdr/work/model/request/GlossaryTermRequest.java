package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 翻译词表维护请求（后台）
 *
 * @author cz
 */
@Schema(description = "翻译词表维护请求")
public record GlossaryTermRequest(
        @Schema(description = "源语言，默认 zh") String sourceLocale,
        @Schema(description = "目标语言，默认 en") String targetLocale,
        @Schema(description = "术语（原文）") String term,
        @Schema(description = "译文") String translation,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark
) {
}
