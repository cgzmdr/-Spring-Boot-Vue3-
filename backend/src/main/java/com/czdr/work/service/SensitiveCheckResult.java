package com.czdr.work.service;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 敏感词检测结果
 */
@Schema(description = "敏感词检测结果")
public record SensitiveCheckResult(
        @Schema(description = "命中等级：none / watch / block") String level,
        @Schema(description = "命中的词（逗号分隔，最多 200 字）") String hitWords,
        @Schema(description = "掩码后的文本；仅当 replace 级词真正被替换时非空，否则为 null（调用方据此决定是否替换原文）") String cleaned
) {
    /** 无命中：cleaned 为 null，调用方保持原文不变 */
    public static SensitiveCheckResult none() {
        return new SensitiveCheckResult("none", null, null);
    }

    public boolean blocked() {
        return "block".equals(level);
    }

    public boolean risky() {
        return !"none".equals(level);
    }
}
