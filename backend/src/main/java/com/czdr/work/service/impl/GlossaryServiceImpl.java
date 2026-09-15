package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.TranslateProperties;
import com.czdr.work.model.entity.TranslateGlossary;
import com.czdr.work.model.request.GlossaryTermRequest;
import com.czdr.work.service.GlossaryService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 翻译词表实现：词条存库、60 秒内存缓存、后台改动立即失效。
 *
 * <p>词条按「术语 + 目标语言」使用，支持两个方向：</p>
 * <ul>
 *   <li>CJK 术语（如 蒙古族）按最长匹配做前缀扫描；</li>
 *   <li>拉丁术语（如 Mongolian）按词边界、忽略大小写匹配，避免 Ha 命中 Handler；</li>
 *   <li>只有正向词条时，反向自动推导（zh→en 的词条同时可用于 en→zh），无需重复维护两遍。</li>
 * </ul>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlossaryServiceImpl implements GlossaryService {

    /** 词表内存缓存时长：后台改动会立刻 refresh()，这里的 TTL 主要防外部直接改库 */
    private static final long CACHE_TTL_MILLIS = 60_000L;
    private static final int MAX_TERM_LENGTH = 128;
    private static final int MAX_TRANSLATION_LENGTH = 256;
    private static final int MAX_LOCALE_LENGTH = 8;
    /** AI 自动沉淀的术语更保守：避免把整句或短语塞进词表 */
    private static final int AUTO_MAX_TERM_LENGTH = 32;
    private static final int AUTO_MAX_TRANSLATION_LENGTH = 64;
    /** AI 自动沉淀的来源标记（后台可按关键词检索复核） */
    public static final String AUTO_REMARK = "AI 自动提取";

    private final EasyEntityQuery entityQuery;
    private final TranslateProperties properties;

    /** 目标语言 -> （首字符 -> 词条，按术语长度倒序） */
    private volatile Map<String, Map<Character, List<Term>>> cache = Map.of();
    private volatile long loadedAt = 0L;

    /**
     * 一条词条。
     *
     * @param term        原文术语
     * @param translation 译文
     * @param lower       小写形式（拉丁术语匹配用）
     * @param latin       是否为拉丁术语（否则按 CJK 前缀匹配）
     */
    private record Term(String term, String translation, String lower, boolean latin) {
    }

    // ---------------------------------------------------------------- 翻译

    @Override
    public String translate(String text, String sourceLocale, String targetLocale) {
        GlossaryMatch match = match(text, sourceLocale, targetLocale);
        return match.accepted() ? match.translated() : null;
    }

    @Override
    public GlossaryMatch match(String text, String sourceLocale, String targetLocale) {
        if (text == null || text.isBlank()) {
            return new GlossaryMatch("", 0d, 0, false);
        }
        Map<Character, List<Term>> buckets = termsFor(targetLocale);
        if (buckets.isEmpty()) {
            return new GlossaryMatch("", 0d, 0, false);
        }
        StringBuilder out = new StringBuilder(text.length() + 64);
        int hits = 0;
        // 命中替换掉的「有效字符」数：用于判断词表是否只译出了半截
        int replaced = 0;
        int effective = 0;
        int i = 0;
        while (i < text.length()) {
            Term matched = matchAt(text, i, buckets);
            if (matched != null) {
                out.append(matched.translation());
                i += matched.term().length();
                replaced += countEffective(matched.term());
                hits++;
            } else {
                int cp = text.codePointAt(i);
                if (isEffective(cp)) {
                    effective++;
                }
                out.append(text.charAt(i));
                i++;
            }
        }
        if (hits == 0) {
            // 一条都没命中视为「词表不适用」，交给兜底提供方，避免返回半截混杂文本
            return new GlossaryMatch("", 0d, 0, false);
        }
        double coverage = effective + replaced == 0 ? 0d : (double) replaced / (effective + replaced);
        boolean accepted = coverage >= properties.glossaryMinCoverage();
        return new GlossaryMatch(accepted ? out.toString() : "", coverage, hits, accepted);
    }

    /** 有效字符 = 字母 / 数字 / CJK（空白与标点不计入覆盖率） */
    private boolean isEffective(int cp) {
        if (Character.isLetterOrDigit(cp)) {
            return true;
        }
        Character.UnicodeScript script = Character.UnicodeScript.of(cp);
        return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL;
    }

    private int countEffective(String term) {
        return (int) term.codePoints().filter(this::isEffective).count();
    }

    private Term matchAt(String text, int index, Map<Character, List<Term>> buckets) {
        char start = Character.toLowerCase(text.charAt(index));
        List<Term> candidates = buckets.get(start);
        if (candidates == null) {
            return null;
        }
        // 已按术语长度倒序，首个命中即最长匹配
        for (Term term : candidates) {
            if (term.latin()) {
                if (text.regionMatches(true, index, term.term(), 0, term.term().length())
                        && boundaryBefore(text, index) && boundaryAfter(text, index + term.term().length())) {
                    return term;
                }
            } else if (text.startsWith(term.term(), index)) {
                return term;
            }
        }
        return null;
    }

    /** 词边界（前后都不是字母/数字），避免 Latin 术语命中更长单词的一部分 */
    private boolean boundaryBefore(String text, int index) {
        return index == 0 || !isWordChar(text.codePointAt(index - 1));
    }

    private boolean boundaryAfter(String text, int index) {
        return index >= text.length() || !isWordChar(text.codePointAt(index));
    }

    private boolean isWordChar(int cp) {
        return Character.isLetterOrDigit(cp);
    }

    @Override
    public Map<String, Integer> locales() {
        ensureLoaded();
        Map<String, Integer> map = new LinkedHashMap<>();
        cache.forEach((locale, buckets) -> {
            int count = buckets.values().stream().mapToInt(List::size).sum();
            if (count > 0) {
                map.put(locale, count);
            }
        });
        return map;
    }

    private Map<Character, List<Term>> termsFor(String targetLocale) {
        String locale = locale(targetLocale);
        if (locale.isEmpty()) {
            return Map.of();
        }
        ensureLoaded();
        return cache.getOrDefault(locale, Map.of());
    }

    @Override
    public int learnTerms(String sourceLocale, String targetLocale, String sourceText, String rawTerms) {
        if (!properties.autoGlossaryEnabled() || rawTerms == null || rawTerms.isBlank() || sourceText == null) {
            return 0;
        }
        String source = locale(sourceLocale).isEmpty() ? "zh" : locale(sourceLocale);
        String target = locale(targetLocale).isEmpty() ? "en" : locale(targetLocale);
        if (source.equals(target)) {
            return 0;
        }
        int added = 0;
        int max = properties.autoGlossaryMaxTerms();
        Set<String> seen = new HashSet<>();
        for (String raw : rawTerms.split("[\\r\\n]+")) {
            if (added >= max) {
                break;
            }
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s*(?:=|＝|→|\\t|:)\\s*", 2);
            if (parts.length < 2) {
                continue;
            }
            // 去掉「1. 」「- 」「* 」这类列表前缀
            String term = parts[0].replaceAll("^[-*•\\s\\d.、)）]+", "").trim();
            String translation = parts[1].replaceAll("^[-*•\\s]+", "").trim();
            if (term.isEmpty() || translation.isEmpty()
                    || term.length() > AUTO_MAX_TERM_LENGTH || translation.length() > AUTO_MAX_TRANSLATION_LENGTH) {
                continue;
            }
            // 术语必须真的出现在原文里，避免模型凭空发挥
            if (!containsTerm(sourceText, term)) {
                continue;
            }
            if (!seen.add(term.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (query(source, target, term) != null) {
                continue;
            }
            TranslateGlossary entity = new TranslateGlossary();
            entity.setId(UUID.randomUUID());
            entity.setSourceLocale(source);
            entity.setTargetLocale(target);
            entity.setTerm(term);
            entity.setTranslation(translation);
            entity.setEnabled(true);
            entity.setRemark(AUTO_REMARK);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
            added++;
        }
        if (added > 0) {
            // 立即生效：后续翻译马上能用上新术语
            refresh();
            log.info("AI 术语沉淀 {} 条（{} -> {}）", added, source, target);
        }
        return added;
    }

    private boolean containsTerm(String text, String term) {
        return text.contains(term) || text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    @Override
    public void refresh() {
        loadedAt = 0L;
    }

    /**
     * 加载词表：一次扫描全部启用词条，同时构建「正向（target=目标语言）」与
     * 「反向（source=目标语言，术语与译文互换）」两个方向的索引；正向优先。
     */
    private void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < CACHE_TTL_MILLIS && !cache.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - loadedAt < CACHE_TTL_MILLIS && !cache.isEmpty()) {
                return;
            }
            Map<String, Map<String, Term>> merged = new HashMap<>();
            try {
                List<TranslateGlossary> rows = entityQuery.queryable(TranslateGlossary.class)
                        .where(t -> t.enabled().eq(true))
                        .toList();
                // 先放反向（术语与译文互换），再用正向覆盖，保证正向词条优先
                for (TranslateGlossary row : rows) {
                    String source = locale(row.getSourceLocale());
                    String target = locale(row.getTargetLocale());
                    if (source.isEmpty() || target.isEmpty() || row.getTerm() == null || row.getTranslation() == null) {
                        continue;
                    }
                    putTerm(merged, target, row.getTerm().trim(), row.getTranslation().trim());
                    putTerm(merged, source, row.getTranslation().trim(), row.getTerm().trim());
                }
            } catch (Exception e) {
                log.warn("加载翻译词表失败，本次不做词表兜底翻译：{}", e.getMessage());
                merged = new HashMap<>();
            }
            Map<String, Map<Character, List<Term>>> next = new HashMap<>();
            merged.forEach((locale, terms) -> {
                Map<Character, List<Term>> buckets = new HashMap<>();
                terms.values().forEach(term -> buckets
                        .computeIfAbsent(Character.toLowerCase(term.term().charAt(0)), k -> new ArrayList<>())
                        .add(term));
                buckets.values().forEach(list -> list.sort(
                        Comparator.comparingInt((Term t) -> t.term().length()).reversed()));
                next.put(locale, buckets);
            });
            cache = next;
            loadedAt = System.currentTimeMillis();
            log.info("翻译词表加载完成：{}", next.keySet());
        }
    }

    /** 同一语言对里术语更长的优先保留（避免被短词覆盖），长度相同时正向先写入者生效 */
    private void putTerm(Map<String, Map<String, Term>> merged, String locale, String term, String translation) {
        if (term.isEmpty() || translation.isEmpty() || term.length() > MAX_TERM_LENGTH) {
            return;
        }
        Map<String, Term> terms = merged.computeIfAbsent(locale, k -> new LinkedHashMap<>());
        String key = term.toLowerCase(Locale.ROOT);
        Term existing = terms.get(key);
        if (existing != null && existing.term().length() >= term.length()) {
            return;
        }
        terms.put(key, new Term(term, translation, key, !isCjkTerm(term)));
    }

    private boolean isCjkTerm(String term) {
        return term.codePoints().anyMatch(cp -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            return script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL;
        });
    }

    // ---------------------------------------------------------------- 后台维护

    @Override
    public EasyPageResult<TranslateGlossary> list(String keyword, String targetLocale, Pageable pageable) {
        String locale = locale(targetLocale);
        return entityQuery.queryable(TranslateGlossary.class)
                .where(t -> {
                    if (keyword != null && !keyword.isBlank()) {
                        t.or(() -> {
                            t.term().like(keyword.trim());
                            t.translation().like(keyword.trim());
                            // 备注也参与检索：便于按「AI 自动提取」复核自动沉淀的术语
                            t.remark().like(keyword.trim());
                        });
                    }
                    if (!locale.isEmpty()) {
                        t.targetLocale().eq(locale);
                    }
                })
                .orderBy(t -> {
                    t.targetLocale().asc();
                    t.term().asc();
                })
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Override
    public String save(GlossaryTermRequest request, String currentTerm) {
        if (request == null || request.term() == null || request.term().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "术语不能为空");
        }
        if (request.translation() == null || request.translation().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "译文不能为空");
        }
        String term = request.term().trim();
        String translation = request.translation().trim();
        if (term.length() > MAX_TERM_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "术语过长（最多 " + MAX_TERM_LENGTH + " 字）");
        }
        if (translation.length() > MAX_TRANSLATION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "译文过长（最多 " + MAX_TRANSLATION_LENGTH + " 字）");
        }
        String source = locale(request.sourceLocale()).isEmpty() ? "zh" : locale(request.sourceLocale());
        String target = locale(request.targetLocale()).isEmpty() ? "en" : locale(request.targetLocale());
        if (source.equals(target)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "源语言与目标语言不能相同");
        }
        TranslateGlossary byNewTerm = query(source, target, term);
        TranslateGlossary existing = byNewTerm;
        boolean update = currentTerm != null && !currentTerm.isBlank();
        if (!update && byNewTerm != null) {
            // 新增路径下术语已存在：明确报重复，避免「新增」静默覆盖已有译文（编辑请带上 currentTerm）
            throw new BusinessException(ErrorCode.DUPLICATE, "该术语已存在于词表中，请改用编辑");
        }
        if (update && !currentTerm.trim().equals(term)) {
            // 重命名：命中「原术语」那一行原地改名
            TranslateGlossary byOldTerm = query(source, target, currentTerm.trim());
            if (byOldTerm != null) {
                existing = byOldTerm;
            }
            if (byNewTerm != null && byNewTerm != byOldTerm) {
                throw new BusinessException(ErrorCode.DUPLICATE, "该术语已存在于词表中");
            }
        }
        boolean enabled = request.enabled() == null || request.enabled();
        if (existing == null) {
            TranslateGlossary entity = new TranslateGlossary();
            entity.setId(UUID.randomUUID());
            entity.setSourceLocale(source);
            entity.setTargetLocale(target);
            entity.setTerm(term);
            entity.setTranslation(translation);
            entity.setEnabled(enabled);
            entity.setRemark(request.remark());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
        } else {
            existing.setTerm(term);
            existing.setTranslation(translation);
            existing.setEnabled(enabled);
            existing.setRemark(request.remark());
            existing.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(existing).executeRows();
        }
        refresh();
        return term;
    }

    @Override
    public void delete(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "词条 ID 不能为空");
        }
        TranslateGlossary entity = entityQuery.queryable(TranslateGlossary.class)
                .where(t -> t.id().eq(id))
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // EasyQuery 删除需显式允许删除语句，否则静默不执行
        entityQuery.deletable(entity).allowDeleteStatement(true).executeRows();
        refresh();
    }

    @Override
    public Map<String, Object> importTerms(String text, String sourceLocale, String targetLocale) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入内容不能为空");
        }
        String source = locale(sourceLocale).isEmpty() ? "zh" : locale(sourceLocale);
        String target = locale(targetLocale).isEmpty() ? "en" : locale(targetLocale);
        if (source.equals(target)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "源语言与目标语言不能相同");
        }
        int added = 0;
        int skipped = 0;
        Set<String> seen = new HashSet<>();
        for (String raw : text.split("[\\r\\n]+")) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // 支持 术语=译文 / 术语→译文 / 术语<TAB>译文 / 全角＝
            String[] parts = line.split("\\s*(?:=|＝|→|\\t)\\s*", 2);
            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                skipped++;
                continue;
            }
            String term = parts[0].trim();
            String translation = parts[1].trim();
            if (term.length() > MAX_TERM_LENGTH || translation.length() > MAX_TRANSLATION_LENGTH
                    || !seen.add(term.toLowerCase(Locale.ROOT)) || query(source, target, term) != null) {
                skipped++;
                continue;
            }
            TranslateGlossary entity = new TranslateGlossary();
            entity.setId(UUID.randomUUID());
            entity.setSourceLocale(source);
            entity.setTargetLocale(target);
            entity.setTerm(term);
            entity.setTranslation(translation);
            entity.setEnabled(true);
            entity.setRemark("批量导入");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
            added++;
        }
        refresh();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        result.put("sourceLocale", source);
        result.put("targetLocale", target);
        return result;
    }

    @Override
    public List<String> exportTerms(String targetLocale) {
        String locale = locale(targetLocale);
        return entityQuery.queryable(TranslateGlossary.class)
                .where(t -> {
                    if (!locale.isEmpty()) {
                        t.targetLocale().eq(locale);
                    }
                })
                .orderBy(t -> t.term().asc())
                .toList()
                .stream()
                .map(t -> t.getTerm() + "=" + t.getTranslation())
                .toList();
    }

    @Override
    public Map<String, Object> stats() {
        List<TranslateGlossary> rows = entityQuery.queryable(TranslateGlossary.class).toList();
        long enabled = rows.stream().filter(t -> Boolean.TRUE.equals(t.getEnabled())).count();
        Map<String, Integer> locales = locales();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("enabled", enabled);
        result.put("locales", locales);
        return result;
    }

    // ---------------------------------------------------------------- 工具

    private TranslateGlossary query(String source, String target, String term) {
        return entityQuery.queryable(TranslateGlossary.class)
                .where(t -> {
                    t.sourceLocale().eq(source);
                    t.targetLocale().eq(target);
                    t.term().eq(term);
                })
                .firstOrNull();
    }

    private String locale(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String locale = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (locale.length() > MAX_LOCALE_LENGTH) {
            locale = locale.substring(0, MAX_LOCALE_LENGTH);
        }
        int dash = locale.indexOf('-');
        return dash > 0 ? locale.substring(0, dash) : locale;
    }
}
