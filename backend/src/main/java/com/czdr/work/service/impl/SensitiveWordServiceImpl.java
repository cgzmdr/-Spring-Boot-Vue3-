package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.SensitiveWord;
import com.czdr.work.model.request.SensitiveWordRequest;
import com.czdr.work.service.SensitiveCheckResult;
import com.czdr.work.service.SensitiveWordService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 本地敏感词实现：
 * · 词库全量缓存在内存中（默认 60 秒过期，或后台改动后立即失效），匹配为 O(n) 包含判断；
 * · block 级命中 → 内容直接进待审；watch 级 → 打标提示；replace 级 → 掩码后仍可见。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final Set<String> LEVELS = Set.of("block", "watch", "replace");
    private static final long CACHE_TTL_MILLIS = 60_000L;
    private static final int MAX_HIT_WORDS_LENGTH = 200;

    private final EasyEntityQuery entityQuery;

    private volatile List<SensitiveWord> cache = List.of();
    private volatile long loadedAt = 0L;

    @Override
    public SensitiveCheckResult check(String text) {
        if (text == null || text.isBlank()) {
            return SensitiveCheckResult.none();
        }
        List<SensitiveWord> words = enabledWords();
        if (words.isEmpty()) {
            return SensitiveCheckResult.none();
        }
        String probe = text.toLowerCase(Locale.ROOT);
        String level = "none";
        String cleaned = text;
        // 只有 replace 级词真正被掩码时才回传 cleaned，避免调用方拿「标题+正文」整体覆盖正文
        boolean masked = false;
        List<String> hits = new ArrayList<>();
        for (SensitiveWord word : words) {
            String value = word.getWord();
            if (value == null || value.isBlank()) {
                continue;
            }
            String needle = value.toLowerCase(Locale.ROOT);
            if (!probe.contains(needle)) {
                continue;
            }
            hits.add(value);
            String wordLevel = word.getLevel() == null ? "watch" : word.getLevel();
            if ("block".equals(wordLevel)) {
                level = "block";
            } else if ("replace".equals(wordLevel)) {
                cleaned = cleaned.replace(value, "*".repeat(value.length()));
                masked = true;
                if (!"block".equals(level) && !"watch".equals(level)) {
                    level = "replace";
                }
            } else if (!"block".equals(level)) {
                level = "watch";
            }
        }
        if (hits.isEmpty()) {
            return SensitiveCheckResult.none();
        }
        String hitWords = String.join("、", hits);
        if (hitWords.length() > MAX_HIT_WORDS_LENGTH) {
            hitWords = hitWords.substring(0, MAX_HIT_WORDS_LENGTH);
        }
        return new SensitiveCheckResult(level, hitWords, masked ? cleaned : null);
    }

    @Override
    public void refresh() {
        loadedAt = 0L;
    }

    @Override
    public EasyPageResult<SensitiveWord> list(String keyword, Pageable pageable) {
        return entityQuery.queryable(SensitiveWord.class)
                .where(w -> {
                    if (keyword != null && !keyword.isBlank()) {
                        w.word().like(keyword.trim());
                    }
                })
                .orderBy(w -> w.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Override
    public List<SensitiveWord> all() {
        return enabledWords();
    }

    @Override
    public String save(SensitiveWordRequest request, String currentWord) {
        if (request == null || request.word() == null || request.word().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "词条不能为空");
        }
        String word = request.word().trim();
        if (word.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "词条过长（最多 64 字）");
        }
        String level = request.level() == null || request.level().isBlank() ? "watch" : request.level().trim();
        if (!LEVELS.contains(level)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "等级不合法（block / watch / replace）");
        }
        String locale = request.locale() == null || request.locale().isBlank() ? "zh" : request.locale().trim();
        SensitiveWord exist = queryByWord(word);
        if (exist != null && (currentWord == null || !currentWord.equals(word))) {
            throw new BusinessException(ErrorCode.DUPLICATE, "该词条已存在");
        }
        if (exist == null) {
            SensitiveWord entity = new SensitiveWord();
            entity.setId(UUID.randomUUID());
            entity.setWord(word);
            entity.setLocale(locale);
            entity.setLevel(level);
            entity.setEnabled(request.enabled() == null || request.enabled());
            entity.setRemark(request.remark());
            entity.setCreatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
        } else {
            exist.setLevel(level);
            exist.setLocale(locale);
            exist.setEnabled(request.enabled() == null || request.enabled());
            exist.setRemark(request.remark());
            entityQuery.updatable(exist).executeRows();
        }
        refresh();
        return word;
    }

    @Override
    public void delete(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "词条 ID 不能为空");
        }
        SensitiveWord entity = entityQuery.queryable(SensitiveWord.class)
                .where(w -> w.id().eq(id))
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // EasyQuery 删除需显式允许删除语句，否则静默不执行
        entityQuery.deletable(entity).allowDeleteStatement(true).executeRows();
        refresh();
    }

    @Override
    @Transactional
    public Map<String, Object> importWords(String text, String level, String locale) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入内容不能为空");
        }
        String safeLevel = level == null || level.isBlank() ? "watch" : level.trim();
        if (!LEVELS.contains(safeLevel)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "等级不合法（block / watch / replace）");
        }
        String safeLocale = locale == null || locale.isBlank() ? "zh" : locale.trim();
        // 支持换行 / 逗号 / 顿号 / 分号分隔，自动去重去空白
        List<String> candidates = List.of(text.split("[\\n,，、;；]+"));
        int added = 0;
        int skipped = 0;
        Set<String> seen = new java.util.HashSet<>();
        for (String raw : candidates) {
            String word = raw == null ? "" : raw.trim();
            if (word.isEmpty() || word.length() > 64 || !seen.add(word)) {
                if (!word.isEmpty()) {
                    skipped++;
                }
                continue;
            }
            if (queryByWord(word) != null) {
                skipped++;
                continue;
            }
            SensitiveWord entity = new SensitiveWord();
            entity.setId(UUID.randomUUID());
            entity.setWord(word);
            entity.setLocale(safeLocale);
            entity.setLevel(safeLevel);
            entity.setEnabled(true);
            entity.setRemark("批量导入");
            entity.setCreatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
            added++;
        }
        refresh();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        result.put("level", safeLevel);
        return result;
    }

    @Override
    public List<String> exportWords() {
        return entityQuery.queryable(SensitiveWord.class)
                .orderBy(w -> w.word().asc())
                .toList()
                .stream()
                .map(SensitiveWord::getWord)
                .toList();
    }

    private SensitiveWord queryByWord(String word) {
        return entityQuery.queryable(SensitiveWord.class)
                .where(w -> w.word().eq(word))
                .firstOrNull();
    }

    /** 加载启用中的词条（60 秒内存缓存，后台改动会立即失效） */
    private List<SensitiveWord> enabledWords() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < CACHE_TTL_MILLIS && !cache.isEmpty()) {
            return cache;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - loadedAt < CACHE_TTL_MILLIS && !cache.isEmpty()) {
                return cache;
            }
            try {
                cache = entityQuery.queryable(SensitiveWord.class)
                        .where(w -> w.enabled().eq(true))
                        .toList();
            } catch (Exception e) {
                log.warn("加载敏感词库失败，本次不做敏感词检测：{}", e.getMessage());
                cache = List.of();
            }
            loadedAt = System.currentTimeMillis();
            return cache;
        }
    }
}
