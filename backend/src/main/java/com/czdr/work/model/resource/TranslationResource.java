package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 机器翻译结果。
 * {@code translated=false} 表示已降级为「仅原文」（未接入翻译服务 / 服务不可用 / 原文即目标语言），
 * 此时 {@code content} 为原文，前端不应展示译文切换。
 *
 * @author cz
 */
@Schema(description = "机器翻译结果")
public record TranslationResource(
        @Schema(description = "内容类型") String targetType,
        @Schema(description = "内容 ID") String targetId,
        @Schema(description = "目标语言") String targetLocale,
        @Schema(description = "原文语言") String sourceLocale,
        @Schema(description = "译文（translated=false 时为原文）") String content,
        @Schema(description = "产出来源：libretranslate / ollama / glossary / none") String provider,
        @Schema(description = "是否翻译成功") boolean translated,
        @Schema(description = "是否命中缓存") boolean cached,
        @Schema(description = "译文失效标记（原文已改动，缓存作废）") boolean stale,
        @Schema(description = "说明文案（降级原因等）") String message
) {
}
