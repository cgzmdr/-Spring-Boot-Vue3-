package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.TranslateProperties;
import com.czdr.work.model.entity.DiscussionBoard;
import com.czdr.work.model.entity.DiscussionConversation;
import com.czdr.work.model.entity.DiscussionMessage;
import com.czdr.work.model.entity.DiscussionPost;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.DiscussionTranslation;
import com.czdr.work.model.resource.TranslateStatusResource;
import com.czdr.work.model.resource.TranslationResource;
import com.czdr.work.service.TranslateService;
import com.czdr.work.service.translate.TranslateProvider;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 机器翻译实现：缓存优先 → 自建服务 → 失败降级为「仅原文」。
 *
 * <p>译文按「原文指纹」失效：正文被编辑后 hash 变化，旧译文不再命中，也不会被覆盖清理，
 * 由运维按需清理（表很小）。缓存表见 {@code V6__translation.sql}。</p>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    public static final String TYPE_TOPIC = "topic";
    public static final String TYPE_POST = "post";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_BOARD = "board";

    private static final Map<String, String> PROVIDER_LABELS = Map.of(
            "none", "未启用翻译",
            "spring-ai", "机器翻译 · AI 大模型",
            "libretranslate", "机器翻译 · LibreTranslate",
            "ollama", "机器翻译 · 本地大模型",
            "glossary", "机器翻译 · 词表兜底"
    );

    private final EasyEntityQuery entityQuery;
    private final TranslateProperties properties;
    private final List<TranslateProvider> providers;

    @Override
    public TranslateStatusResource status() {
        String provider = properties.provider();
        boolean mainReady = properties.enabled() && providerOf(provider) != null;
        boolean fallbackReady = properties.fallbackEnabled() && providerOf(properties.fallbackProvider()) != null;
        return new TranslateStatusResource(
                mainReady || fallbackReady,
                provider,
                PROVIDER_LABELS.getOrDefault(provider, "机器翻译"),
                properties.cacheEnabled(),
                fallbackReady ? properties.fallbackProvider() : "none");
    }

    @Override
    public TranslationResource translate(String targetType, UUID targetId, String targetLocale, String scope, UUID viewerId) {
        String type = targetType == null ? "" : targetType.trim().toLowerCase(Locale.ROOT);
        if (!List.of(TYPE_TOPIC, TYPE_POST, TYPE_MESSAGE, TYPE_BOARD).contains(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的内容类型：" + targetType);
        }
        String locale = targetLocale == null ? "" : targetLocale.trim().toLowerCase(Locale.ROOT);
        if (locale.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少目标语言");
        }
        String mode = scope == null || scope.isBlank() ? "all" : scope.trim().toLowerCase(Locale.ROOT);

        Source source = loadSource(type, targetId, mode, viewerId);
        if (source.text() == null || source.text().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或不可见");
        }
        if (sameLanguage(source.locale(), locale)) {
            return degraded(type, targetId, locale, source, "原文已是该语言");
        }
        if (source.text().length() > properties.maxChars()) {
            return degraded(type, targetId, locale, source, "内容过长，暂不提供翻译");
        }

        String hash = sha256(source.text());
        if (properties.cacheEnabled()) {
            DiscussionTranslation cached = entityQuery.queryable(DiscussionTranslation.class)
                    .where(t -> {
                        t.targetType().eq(type);
                        t.targetId().eq(targetId);
                        t.targetLocale().eq(locale);
                        t.sourceHash().eq(hash);
                    })
                    .firstOrNull();
            if (cached != null) {
                return new TranslationResource(type, targetId.toString(), locale, source.locale(),
                        cached.getContent(), cached.getProvider(), true, true, false, null);
            }
        }

        TranslateProvider provider = providerOf(properties.provider());
        if (provider == null) {
            return degraded(type, targetId, locale, source, "站点尚未接入翻译服务，已显示原文");
        }
        String translated = null;
        String usedBy = null;
        try {
            translated = provider.translate(source.text(), source.locale(), locale);
            usedBy = provider.name();
        } catch (Exception e) {
            log.warn("翻译失败（{} · {}）：{}", type, provider.name(), e.getMessage());
        }
        // 主提供方失败（超时 / 额度 / 断网）时尝试兜底提供方（默认本地词表），仍失败才降级为仅原文
        if ((translated == null || translated.isBlank()) && properties.fallbackEnabled()) {
            TranslateProvider fallback = providerOf(properties.fallbackProvider());
            if (fallback != null) {
                try {
                    translated = fallback.translate(source.text(), source.locale(), locale);
                    usedBy = fallback.name();
                    log.info("翻译已回退到兜底提供方：{}", usedBy);
                } catch (Exception e) {
                    log.warn("兜底翻译失败（{} · {}）：{}", type, fallback.name(), e.getMessage());
                }
            }
        }
        if (translated == null || translated.isBlank()) {
            return degraded(type, targetId, locale, source, "翻译服务暂不可用，已显示原文");
        }
        if (properties.cacheEnabled()) {
            saveCache(type, targetId, locale, source, hash, translated, usedBy);
        }
        return new TranslationResource(type, targetId.toString(), locale, source.locale(),
                translated, usedBy, true, false, false, null);
    }

    // ---------------------------------------------------------------- 原文读取

    private record Source(String text, String locale) {
    }

    private Source loadSource(String type, UUID id, String scope, UUID viewerId) {
        return switch (type) {
            case TYPE_TOPIC -> {
                DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                        .where(t -> t.id().eq(id))
                        .firstOrNull();
                if (topic == null || !visible(topic.getStatus())) {
                    yield new Source(null, null);
                }
                String text = switch (scope) {
                    case "title" -> topic.getTitle();
                    case "body" -> topic.getContent();
                    default -> topic.getTitle() + "\n\n" + topic.getContent();
                };
                yield new Source(text, topic.getLang());
            }
            case TYPE_POST -> {
                DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                        .where(p -> p.id().eq(id))
                        .firstOrNull();
                if (post == null || !visible(post.getStatus())) {
                    yield new Source(null, null);
                }
                yield new Source(post.getContent(), post.getLang());
            }
            case TYPE_MESSAGE -> {
                DiscussionMessage message = entityQuery.queryable(DiscussionMessage.class)
                        .where(m -> m.id().eq(id))
                        .firstOrNull();
                if (message == null || Boolean.TRUE.equals(message.getRecalled())) {
                    yield new Source(null, null);
                }
                // 私信译文只对会话双方开放
                DiscussionConversation conversation = entityQuery.queryable(DiscussionConversation.class)
                        .where(c -> c.id().eq(message.getConversationId()))
                        .firstOrNull();
                if (conversation == null || viewerId == null
                        || (!viewerId.equals(conversation.getUserA()) && !viewerId.equals(conversation.getUserB()))) {
                    yield new Source(null, null);
                }
                yield new Source(message.getContent(), message.getLang());
            }
            case TYPE_BOARD -> {
                DiscussionBoard board = entityQuery.queryable(DiscussionBoard.class)
                        .where(b -> b.id().eq(id))
                        .firstOrNull();
                if (board == null) {
                    yield new Source(null, null);
                }
                String text = board.getDescription() == null || board.getDescription().isBlank()
                        ? board.getName()
                        : board.getName() + "\n\n" + board.getDescription();
                yield new Source(text, "zh");
            }
            default -> new Source(null, null);
        };
    }

    private boolean visible(String status) {
        return "published".equals(status);
    }

    // ---------------------------------------------------------------- 缓存与降级

    private void saveCache(String type, UUID targetId, String locale, Source source, String hash,
                           String content, String provider) {
        try {
            // 并发重复请求可能撞唯一键：忽略冲突即可（译文一致）
            DiscussionTranslation existing = entityQuery.queryable(DiscussionTranslation.class)
                    .where(t -> {
                        t.targetType().eq(type);
                        t.targetId().eq(targetId);
                        t.targetLocale().eq(locale);
                        t.sourceHash().eq(hash);
                    })
                    .firstOrNull();
            if (existing != null) {
                return;
            }
            DiscussionTranslation entity = new DiscussionTranslation();
            entity.setId(UUID.randomUUID());
            entity.setTargetType(type);
            entity.setTargetId(targetId);
            entity.setTargetLocale(locale);
            entity.setSourceLocale(source.locale());
            entity.setSourceHash(hash);
            entity.setSourceExcerpt(excerpt(source.text()));
            entity.setContent(content);
            entity.setProvider(provider);
            entity.setCreatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
        } catch (Exception e) {
            log.warn("写入译文缓存失败（不影响本次返回）：{}", e.getMessage());
        }
    }

    private TranslationResource degraded(String type, UUID targetId, String locale, Source source, String message) {
        return new TranslationResource(type, targetId.toString(), locale, source.locale(),
                source.text(), "none", false, false, false, message);
    }

    private TranslateProvider providerOf(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Map<String, TranslateProvider> map = providers.stream()
                .collect(Collectors.toMap(TranslateProvider::name, Function.identity(), (a, b) -> a));
        return map.get(name.trim().toLowerCase(Locale.ROOT));
    }

    private boolean sameLanguage(String source, String target) {
        if (source == null || source.isBlank()) {
            return false;
        }
        return prefix(source).equals(prefix(target));
    }

    private String prefix(String locale) {
        String value = locale.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        int dash = value.indexOf('-');
        return dash > 0 ? value.substring(0, dash) : value;
    }

    private String excerpt(String text) {
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > 120 ? flat.substring(0, 120) : flat;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "计算原文指纹失败");
        }
    }
}
