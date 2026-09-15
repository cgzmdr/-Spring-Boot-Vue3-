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
 * LibreTranslate（自托管）提供方：{@code POST {base-url}/translate}，
 * 参数 {@code q / source / target / format=text}，可选 {@code api_key}。
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibreTranslateProvider implements TranslateProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        return "libretranslate";
    }

    @Override
    public String translate(String text, String sourceLocale, String targetLocale) {
        String base = properties.baseUrl();
        if (base.isBlank()) {
            throw new TranslateException("未配置 app.translate.base-url");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("q", text);
        payload.put("source", sourceLocale == null || sourceLocale.isBlank() ? "auto" : sourceLocale);
        payload.put("target", targetLocale);
        payload.put("format", "text");
        if (!properties.apiKey().isEmpty()) {
            payload.put("api_key", properties.apiKey());
        }
        String body;
        try {
            body = OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new TranslateException("构造翻译请求失败", e);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/translate"))
                .header("Content-Type", "application/json; charset=utf-8")
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new TranslateException("LibreTranslate 返回 " + response.statusCode());
            }
            JsonNode node = OBJECT_MAPPER.readTree(response.body());
            JsonNode translated = node.get("translatedText");
            if (translated == null || translated.asText().isBlank()) {
                throw new TranslateException("LibreTranslate 未返回译文");
            }
            return translated.asText();
        } catch (TranslateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslateException("翻译请求被中断", e);
        } catch (Exception e) {
            throw new TranslateException("连接 LibreTranslate 失败：" + e.getMessage(), e);
        }
    }
}
