package com.czdr.work.service.translate;

import com.czdr.work.config.TranslateProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ollama（本地大模型）提供方：{@code POST {base-url}/api/generate}，{@code stream=false}。
 * 提示词固定为「只输出译文，不要解释」，避免模型输出多余说明文字。
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaTranslateProvider implements TranslateProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PROMPT_TEMPLATE = """
            你是专业翻译。把下面的文本翻译成%s，只输出译文，不要解释、不要加引号、不要保留原文。
            保留原有换行结构。

            文本：
            %s""";

    private final TranslateProperties properties;

    private HttpClient client;

    @PostConstruct
    void init() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(5, properties.timeoutSeconds())))
                .build();
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public String translate(String text, String sourceLocale, String targetLocale) {
        String base = properties.baseUrl();
        if (base.isBlank()) {
            throw new TranslateException("未配置 app.translate.base-url");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.model());
        payload.put("prompt", PROMPT_TEMPLATE.formatted(languageName(targetLocale), text));
        payload.put("stream", false);
        String body;
        try {
            body = OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new TranslateException("构造翻译请求失败", e);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/generate"))
                .header("Content-Type", "application/json; charset=utf-8")
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new TranslateException("Ollama 返回 " + response.statusCode());
            }
            JsonNode node = OBJECT_MAPPER.readTree(response.body());
            JsonNode translated = node.get("response");
            if (translated == null || translated.asText().isBlank()) {
                throw new TranslateException("Ollama 未返回译文");
            }
            return translated.asText().strip();
        } catch (TranslateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslateException("翻译请求被中断", e);
        } catch (Exception e) {
            throw new TranslateException("连接 Ollama 失败：" + e.getMessage(), e);
        }
    }

    private String languageName(String locale) {
        if (locale == null) {
            return "中文";
        }
        String value = locale.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.startsWith("zh")) {
            return "中文";
        }
        if (value.startsWith("en")) {
            return "英文";
        }
        return locale;
    }
}
