package com.czdr.work.service;

import com.czdr.work.config.TranslateProperties;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Food;
import com.czdr.work.service.translate.TranslateProvider;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * 英文正文回填（方向 C-3）。
 *
 * <p>把 festival / art / food 三类内容的中文 {@code description} 用站点**既有的翻译通道**
 * （{@code app.translate.provider}，开发/生产均配为 spring-ai → DeepSeek）译成英文，
 * 写入 V15 迁移新增的 {@code description_en} 字段，并标记来源为 {@code machine}。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>复用站点通道而非另起客户端</b>：走 {@link TranslateProvider}（按 name 取用
 *       {@code spring-ai} 或 {@code glossary}），因此术语表沉淀、超时、重试策略与线上一致。</li>
 *   <li><b>可续跑</b>：已译且中文未变的行跳过，支持中途中断后重跑。</li>
 *   <li><b>不覆盖人工成果</b>：{@code description_en_source} 为 reviewed/manual 的行不覆盖。</li>
 *   <li><b>不编造</b>：翻译失败的行保持 NULL，不做任何占位填充，前端回退显示中文。</li>
 * </ul>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DescriptionEnBackfillService {

    /** 支持的内容类型 → 中文类型名 */
    public static final List<String> TYPES = List.of("festival", "art", "food");

    private final EasyEntityQuery entityQuery;
    private final List<TranslateProvider> providers;
    private final TranslateProperties properties;

    /** 单条内容的处理结果 */
    public record ItemResult(String type, String id, String name, boolean translated,
                             int zhChars, int enChars, String error) {
    }

    /** 整体统计 */
    public record BatchResult(String type, int scanned, int translated, int skipped, int failed,
                              int zhChars, int enChars, List<ItemResult> failures) {
    }

    /**
     * 回填某一类内容。
     *
     * @param type     festival / art / food
     * @param limit    本次最多处理条数（0 或负数表示不限制）
     * @param dryRun   true 只翻译不写库，用于试跑校验译文
     */
    public BatchResult backfill(String type, int limit, boolean dryRun) {
        String t = type == null ? "" : type.trim().toLowerCase();
        if (!TYPES.contains(t)) {
            throw new IllegalArgumentException("不支持的类别：" + type);
        }
        // 与站点 TranslateServiceImpl 的降级顺序保持一致：主通道失败再试兜底通道。
        // 默认配置下 provider=glossary（本地词表，只能逐词替换、长句必然不达标），
        // fallback-provider=spring-ai（AI 大模型）——真正干活的是后者，
        // 若只按 properties.provider() 取通道，正文会全部翻译失败。
        List<TranslateProvider> chain = new ArrayList<>();
        TranslateProvider primary = providerOf(properties.provider());
        if (primary != null) {
            chain.add(primary);
        }
        if (properties.fallbackEnabled()) {
            TranslateProvider fallback = providerOf(properties.fallbackProvider());
            if (fallback != null) {
                chain.add(fallback);
            }
        }
        if (chain.isEmpty()) {
            throw new IllegalStateException("站点未启用任何翻译通道（app.translate.provider="
                    + properties.provider() + "，fallback=" + properties.fallbackProvider() + "）");
        }

        List<Row> rows = loadRows(t);
        int scanned = 0, translated = 0, skipped = 0, failed = 0, zhChars = 0, enChars = 0;
        /** 真正调用翻译通道的条数：limit 限制的是「本次工作量」而不是「遍历行数」。
         *  否则前几轮已译的行会持续吃掉配额，导致每批都在原地打转、永远推进不到后面的待译行。 */
        int attempted = 0;
        List<ItemResult> failures = new ArrayList<>();

        for (Row row : rows) {
            if (limit > 0 && attempted >= limit) {
                break;
            }
            scanned++;

            String zh = row.description() == null ? "" : row.description().strip();
            if (zh.isEmpty()) {
                skipped++;
                continue;
            }
            // 已有人工成果（reviewed/manual）不覆盖
            if (row.enSource() != null && !"machine".equals(row.enSource())) {
                skipped++;
                continue;
            }
            // 已译且中文未变（有英文即视为已译，重跑时不重复调用模型）
            if (row.descriptionEn() != null && !row.descriptionEn().isBlank()) {
                skipped++;
                continue;
            }
            if (zh.length() > properties.maxChars()) {
                attempted++;
                String msg = "中文正文超过单次翻译上限 " + properties.maxChars() + " 字符（实际 " + zh.length() + "）";
                failed++;
                failures.add(new ItemResult(t, row.id(), row.name(), false, zh.length(), 0, msg));
                log.warn("跳过 {} {}：{}", t, row.name(), msg);
                continue;
            }

            attempted++;

            try {
                String en = null;
                String usedBy = null;
                List<String> errors = new ArrayList<>();
                for (TranslateProvider p : chain) {
                    try {
                        String candidate = p.translate(zh, "zh", "en");
                        if (candidate != null && !candidate.isBlank()) {
                            en = candidate.strip();
                            usedBy = p.name();
                            break;
                        }
                        errors.add(p.name() + ": 返回空译文");
                    } catch (Exception ex) {
                        errors.add(p.name() + ": " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
                    }
                }
                if (en == null) {
                    throw new IllegalStateException(String.join(" | ", errors));
                }
                if (!dryRun) {
                    persist(t, row.id(), en);
                }
                translated++;
                zhChars += zh.length();
                enChars += en.length();
                log.info("已翻译 {} {}（{} 字 → {} 字符，通道 {}）", t, row.name(), zh.length(), en.length(), usedBy);
            } catch (Exception e) {
                failed++;
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                failures.add(new ItemResult(t, row.id(), row.name(), false, zh.length(), 0, msg));
                log.warn("翻译失败 {} {}：{}", t, row.name(), msg);
            }
        }
        return new BatchResult(t, scanned, translated, skipped, failed, zhChars, enChars, failures);
    }

    /** 待翻译数量统计（不调用模型） */
    public Map<String, Object> pending() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String t : TYPES) {
            List<Row> rows = loadRows(t);
            long todo = rows.stream().filter(r -> {
                if (r.description() == null || r.description().isBlank()) return false;
                if (r.enSource() != null && !"machine".equals(r.enSource())) return false;
                return r.descriptionEn() == null || r.descriptionEn().isBlank();
            }).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("total", rows.size());
            m.put("pending", todo);
            m.put("done", rows.size() - todo);
            out.put(t, m);
        }
        return out;
    }

    // ------------------------------------------------------------------ 数据读取与写入

    /** 内容行（跨三表统一视图） */
    private record Row(String id, String name, String description, String descriptionEn, String enSource) {
    }

    private List<Row> loadRows(String type) {
        return switch (type) {
            case "festival" -> entityQuery.queryable(Festival.class)
                    .orderBy(f -> f.orderNum().asc())
                    .toList()
                    .stream()
                    .map(e -> new Row(e.getId().toString(), e.getName(), e.getDescription(),
                            e.getDescriptionEn(), e.getDescriptionEnSource()))
                    .toList();
            case "art" -> entityQuery.queryable(Art.class)
                    .orderBy(a -> a.orderNum().asc())
                    .toList()
                    .stream()
                    .map(e -> new Row(e.getId().toString(), e.getName(), e.getDescription(),
                            e.getDescriptionEn(), e.getDescriptionEnSource()))
                    .toList();
            case "food" -> entityQuery.queryable(Food.class)
                    .orderBy(f -> f.orderNum().asc())
                    .toList()
                    .stream()
                    .map(e -> new Row(e.getId().toString(), e.getName(), e.getDescription(),
                            e.getDescriptionEn(), e.getDescriptionEnSource()))
                    .toList();
            default -> List.of();
        };
    }

    private void persist(String type, String id, String en) {
        UUID uuid = UUID.fromString(id);
        // 沿用项目既有写法：取实体 → 改字段 → updatable(entity)
        switch (type) {
            case "festival" -> {
                Festival e = entityQuery.queryable(Festival.class).where(f -> f.id().eq(uuid)).firstOrNull();
                if (e == null) {
                    throw new IllegalStateException("节日不存在：" + id);
                }
                e.setDescriptionEn(en);
                e.setDescriptionEnSource("machine");
                entityQuery.updatable(e).executeRows();
            }
            case "art" -> {
                Art e = entityQuery.queryable(Art.class).where(a -> a.id().eq(uuid)).firstOrNull();
                if (e == null) {
                    throw new IllegalStateException("艺术不存在：" + id);
                }
                e.setDescriptionEn(en);
                e.setDescriptionEnSource("machine");
                entityQuery.updatable(e).executeRows();
            }
            case "food" -> {
                Food e = entityQuery.queryable(Food.class).where(f -> f.id().eq(uuid)).firstOrNull();
                if (e == null) {
                    throw new IllegalStateException("美食不存在：" + id);
                }
                e.setDescriptionEn(en);
                e.setDescriptionEnSource("machine");
                entityQuery.updatable(e).executeRows();
            }
            default -> throw new IllegalArgumentException("不支持的类别：" + type);
        }
    }

    private TranslateProvider providerOf(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Map<String, TranslateProvider> map = providers.stream()
                .collect(java.util.stream.Collectors.toMap(TranslateProvider::name, Function.identity(), (a, b) -> a));
        return map.get(name.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
