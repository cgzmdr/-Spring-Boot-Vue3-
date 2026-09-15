package com.czdr.work.service.translate;

import com.czdr.work.config.AiConfig;
import com.czdr.work.config.TranslateProperties;
import com.czdr.work.service.GlossaryService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Spring AI 提供方：复用 {@code AiConfig#translateChatClient}（模型 / base-url / key 全部走
 * {@code spring.ai.openai.*}，系统提示词为「只输出译文」的翻译专用提示词）。
 *
 * <p>这是站点的主力翻译通道（开发环境默认）：自建服务 LibreTranslate/Ollama 之外的
 * 「已有大模型接入」方案。调用失败/超时一律抛 {@link TranslateException}，由上层降级。</p>
 *
 * @author cz
 */
@Slf4j
@Component
public class SpringAiTranslateProvider implements TranslateProvider {

    private final TranslateProperties properties;
    /**
     * 用 ObjectProvider 延迟解析 AiConfig 的翻译专用 ChatClient：
     * 站点未配置 spring.ai.openai（例如只跑词表兜底）时，只有真正调用本提供方才会去创建模型 Bean，
     * 失败会被转换成 {@link TranslateException} 并降级，而不是让 /translate/status 直接报错。
     */
    private final ObjectProvider<ChatClient> chatClients;
    /** 术语沉淀：AI 译文里的术语会实时写回词表，下一次翻译即可命中 */
    private final GlossaryService glossaryService;

    /** 虚拟线程执行器：把带超时的模型调用与请求线程解耦，超时后不再阻塞页面 */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SpringAiTranslateProvider(TranslateProperties properties,
                                     @Qualifier("translateChatClient") ObjectProvider<ChatClient> translateChatClients,
                                     GlossaryService glossaryService) {
        this.properties = properties;
        this.chatClients = translateChatClients;
        this.glossaryService = glossaryService;
    }

    @Override
    public String name() {
        return "spring-ai";
    }

    /** 解析 ChatClient（首次调用会触发 OpenAiChatModel 初始化） */
    private ChatClient chatClient() {
        try {
            ChatClient client = chatClients.getIfAvailable();
            if (client == null) {
                throw new TranslateException("站点未配置 spring.ai.openai（缺少 ChatClient）");
            }
            return client;
        } catch (TranslateException e) {
            throw e;
        } catch (Exception e) {
            throw new TranslateException("Spring AI 初始化失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String translate(String text, String sourceLocale, String targetLocale) {
        ChatClient client = chatClient();
        String prompt = buildPrompt(text, sourceLocale, targetLocale);
        int timeout = properties.timeoutSeconds();
        Future<String> future = executor.submit(() -> client.prompt().user(prompt).call().content());
        String content;
        try {
            content = future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TranslateException("Spring AI 翻译超时（" + timeout + " 秒）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslateException("翻译请求被中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new TranslateException("Spring AI 翻译失败：" + cause.getMessage(), cause);
        }
        String cleaned = stripFence(content);
        if (cleaned.isBlank()) {
            throw new TranslateException("Spring AI 未返回译文");
        }
        // 译文与术语表分离：术语沉淀进词表（实时优化），返回给前端的只有译文
        int marker = cleaned.lastIndexOf(AiConfig.TERMS_MARKER);
        String translation = marker < 0 ? cleaned : cleaned.substring(0, marker).strip();
        if (!properties.autoGlossaryEnabled() || marker < 0) {
            return translation.isBlank() ? cleaned : translation;
        }
        String rawTerms = cleaned.substring(marker + AiConfig.TERMS_MARKER.length());
        try {
            int learned = glossaryService.learnTerms(sourceLocale, targetLocale, text, rawTerms);
            if (learned > 0) {
                log.debug("AI 翻译沉淀术语 {} 条", learned);
            }
        } catch (Exception e) {
            // 沉淀失败不影响本次翻译结果
            log.warn("AI 术语沉淀失败（忽略）：{}", e.getMessage());
        }
        return translation.isBlank() ? cleaned : translation;
    }

    private String buildPrompt(String text, String sourceLocale, String targetLocale) {
        return """
                目标语言：%s（%s）
                原文语言：%s

                文本：
                %s""".formatted(languageName(targetLocale), targetLocale,
                languageName(sourceLocale), text);
    }

    private String languageName(String locale) {
        if (locale == null || locale.isBlank()) {
            return "未知";
        }
        String value = locale.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("zh")) {
            return "中文";
        }
        if (value.startsWith("en")) {
            return "英文";
        }
        return locale;
    }

    /** 少数模型会习惯性用 ``` 包裹译文，这里剥掉围栏 */
    private String stripFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstBreak = trimmed.indexOf('\n');
        if (firstBreak < 0) {
            return trimmed.replace("```", "").strip();
        }
        String body = trimmed.substring(firstBreak + 1);
        int closing = body.lastIndexOf("```");
        if (closing >= 0) {
            body = body.substring(0, closing);
        }
        return body.strip();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
