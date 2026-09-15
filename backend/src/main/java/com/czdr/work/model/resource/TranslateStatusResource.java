package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 翻译能力开关（前端据此决定是否显示「译」按钮）。
 *
 * @author cz
 */
@Schema(description = "翻译能力状态")
public record TranslateStatusResource(
        @Schema(description = "是否已启用翻译") boolean enabled,
        @Schema(description = "当前提供方：none / spring-ai / libretranslate / ollama / glossary") String provider,
        @Schema(description = "前端展示用的提供方文案") String label,
        @Schema(description = "缓存是否开启") boolean cacheEnabled,
        @Schema(description = "兜底提供方（主提供方失败时使用）：none 表示失败即降级为仅原文") String fallbackProvider
) {
}
