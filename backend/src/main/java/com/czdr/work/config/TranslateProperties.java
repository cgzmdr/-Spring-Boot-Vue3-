package com.czdr.work.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 机器翻译配置（自建服务，不依赖商业翻译 API）。
 *
 * <pre>
 * app:
 *   translate:
 *     provider: none            # none 关闭 / libretranslate / ollama / glossary
 *     base-url: http://127.0.0.1:5000
 *     api-key:                  # 可选，LibreTranslate 支持
 *     timeout-seconds: 8
 *     max-chars: 4000
 *     cache-enabled: true
 * </pre>
 *
 * @author cz
 */
@Slf4j
@Component
public class TranslateProperties {

    /** none / libretranslate / ollama / glossary */
    @Value("${app.translate.provider:none}")
    private String provider;

    @Value("${app.translate.base-url:}")
    private String baseUrl;

    @Value("${app.translate.api-key:}")
    private String apiKey;

    /** Ollama 使用的本地模型名 */
    @Value("${app.translate.model:qwen2.5:7b}")
    private String model;

    @Value("${app.translate.timeout-seconds:8}")
    private int timeoutSeconds;

    /** 单次翻译的原文上限（超出部分截断，避免自建服务被长文拖垮） */
    @Value("${app.translate.max-chars:4000}")
    private int maxChars;

    /**
     * 词表可接受的最低覆盖率：词表命中替换掉的正文字符 / 正文有效字符（不含空白与标点）。
     * 低于该比例说明词表只能译出半截，直接让给兜底提供方（通常是 AI），避免展示混杂文本。
     */
    @Value("${app.translate.glossary-min-coverage:0.8}")
    private double glossaryMinCoverage;

    /** AI 翻译时是否把译文里的术语自动沉淀进词表（实时优化词表，提升后续准确率） */
    @Value("${app.translate.auto-glossary-enabled:true}")
    private boolean autoGlossaryEnabled;

    /** 单次 AI 翻译最多沉淀的术语条数 */
    @Value("${app.translate.auto-glossary-max-terms:8}")
    private int autoGlossaryMaxTerms;

    @Value("${app.translate.cache-enabled:true}")
    private boolean cacheEnabled;

    /**
     * 兜底提供方：主提供方调用失败（超时/额度/断网）时再试一次；
     * 默认 glossary（本地词表，零依赖），设 none 表示失败即降级为「仅显示原文」。
     */
    @Value("${app.translate.fallback-provider:glossary}")
    private String fallbackProvider;

    public String provider() {
        return provider == null ? "none" : provider.trim().toLowerCase(Locale.ROOT);
    }

    /** 兜底提供方；与主提供方相同或为 none 时视为不启用兜底 */
    public String fallbackProvider() {
        String value = fallbackProvider == null ? "none" : fallbackProvider.trim().toLowerCase(Locale.ROOT);
        return value.isBlank() ? "none" : value;
    }

    public boolean fallbackEnabled() {
        String fallback = fallbackProvider();
        return !"none".equals(fallback) && !fallback.equals(provider());
    }

    public boolean enabled() {
        return !"none".equals(provider()) && !provider().isBlank();
    }

    public String baseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String url = baseUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String apiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public String model() {
        return model == null || model.isBlank() ? "qwen2.5:7b" : model.trim();
    }

    public int timeoutSeconds() {
        return timeoutSeconds <= 0 ? 8 : timeoutSeconds;
    }

    public int maxChars() {
        return maxChars <= 0 ? 4000 : maxChars;
    }

    public double glossaryMinCoverage() {
        if (glossaryMinCoverage <= 0) {
            return 0d;
        }
        return Math.min(1d, glossaryMinCoverage);
    }

    public boolean autoGlossaryEnabled() {
        return autoGlossaryEnabled;
    }

    public int autoGlossaryMaxTerms() {
        return autoGlossaryMaxTerms <= 0 ? 8 : autoGlossaryMaxTerms;
    }

    public boolean cacheEnabled() {
        return cacheEnabled;
    }
}
