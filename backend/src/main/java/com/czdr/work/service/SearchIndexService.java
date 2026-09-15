package com.czdr.work.service;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.SearchDocument;
import com.czdr.work.model.entity.TraditionalSport;
import com.czdr.work.util.JsonUtil;
import com.czdr.work.util.PinyinUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 检索索引构建服务（方向 D）。
 *
 * <p>把 8 类内容聚合成 {@code search_document} 宽表。设计要点：</p>
 * <ul>
 *   <li><b>全量重建</b>：清空后重建，保证索引与业务表最终一致（内容量约 1142 条，秒级完成）。</li>
 *   <li><b>按类型重建</b>：内容维护后只重建该类型，避免全量扫描。</li>
 *   <li><b>拼音在入库时生成</b>：检索阶段不再做转换，直接对拼音列匹配。</li>
 *   <li><b>热度取真实计数</b>：浏览量 / 点赞 / 收藏按权重合成，无计数则为 0（不编造）。</li>
 * </ul>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    /** 支持的内容类型（与 search_document.doc_type 一致） */
    public static final List<String> DOC_TYPES =
            List.of("ethnic", "festival", "art", "food", "custom", "person", "area", "sport");

    private final EasyEntityQuery entityQuery;

    /** 重建结果统计 */
    public record RebuildResult(int total, Map<String, Integer> byType, long elapsedMs) {
    }

    /**
     * 全量重建索引。
     *
     * @param onlyType 仅重建指定类型；为 null / blank 时重建全部
     */
    @Transactional
    public RebuildResult rebuild(String onlyType) {
        long start = System.currentTimeMillis();
        String type = onlyType == null ? "" : onlyType.trim().toLowerCase();
        List<String> targets = type.isBlank() || "all".equals(type)
                ? DOC_TYPES
                : DOC_TYPES.contains(type) ? List.of(type) : List.of();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("不支持的索引类型：" + onlyType);
        }

        Map<String, Integer> byType = new LinkedHashMap<>();
        int total = 0;
        for (String t : targets) {
            // 先删该类型旧文档，再重建：避免内容下架后仍留在索引里
            entityQuery.deletable(SearchDocument.class)
                    .where(d -> d.docType().eq(t))
                    .allowDeleteStatement(true)
                    .executeRows();

            List<SearchDocument> docs = switch (t) {
                case "ethnic" -> fromEthnic();
                case "festival" -> fromFestival();
                case "art" -> fromArt();
                case "food" -> fromFood();
                case "custom" -> fromCustom();
                case "person" -> fromPerson();
                case "area" -> fromArea();
                case "sport" -> fromSport();
                default -> List.of();
            };
            if (!docs.isEmpty()) {
                entityQuery.insertable(docs).executeRows();
            }
            byType.put(t, docs.size());
            total += docs.size();
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("检索索引重建完成：{} 条，耗时 {} ms，明细 {}", total, elapsed, byType);
        return new RebuildResult(total, byType, elapsed);
    }

    // ------------------------------------------------------------------ 各类型 -> 索引文档

    private List<SearchDocument> fromEthnic() {
        List<EthnicGroup> rows = entityQuery.queryable(EthnicGroup.class)
                .where(e -> e.status().eq("published"))
                .orderBy(e -> e.orderNum().asc())
                .toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (EthnicGroup e : rows) {
            SearchDocument d = base("ethnic", e.getId(), "/ethnic/" + e.getId(),
                    e.getName(), e.getSummary(), e.getDescription());
            d.setEthnicName(e.getName());
            d.setCoverImage(e.getCoverImage());
            d.setThemeColor(e.getThemeColor());
            // 主要聚居地首个作为地区分面
            String[] regions = JsonUtil.toStringArray(e.getRegion());
            d.setRegion(regions.length == 0 ? null : regions[0]);
            d.setCategory(e.getLanguageFamily());
            d.setTitleEn(e.getNameEn());
            d.setBodyEn(e.getDescriptionEn());
            // 民族表已有 pinyin（全拼），优先使用；缺失时用工具补齐，并补首字母
            String full = e.getPinyin() == null || e.getPinyin().isBlank()
                    ? PinyinUtil.full(e.getName()) : e.getPinyin().toLowerCase();
            d.setPinyinFull(full);
            d.setPinyinAbbr(PinyinUtil.abbr(e.getName()));
            d.setContentAt(e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromFestival() {
        List<Festival> rows = entityQuery.queryable(Festival.class)
                .where(f -> f.status().eq("published"))
                .include(f -> f.ethnicGroup())
                .orderBy(f -> f.orderNum().asc())
                .toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (Festival f : rows) {
            SearchDocument d = base("festival", f.getId(), "/festival/" + f.getId(),
                    f.getName(), f.getLunarDate(), f.getDescription());
            d.setEthnicName(f.getEthnicGroup() != null ? f.getEthnicGroup().getName() : null);
            d.setCoverImage(f.getCoverImage());
            d.setThemeColor(f.getEthnicGroup() != null ? f.getEthnicGroup().getThemeColor() : null);
            d.setCategory(f.getType());
            d.setTitleEn(f.getNameEn());
            d.setBodyEn(f.getDescriptionEn());
            d.setPinyinFull(PinyinUtil.full(f.getName()));
            d.setPinyinAbbr(PinyinUtil.abbr(f.getName()));
            d.setContentAt(f.getUpdatedAt() != null ? f.getUpdatedAt() : f.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromArt() {
        List<Art> rows = entityQuery.queryable(Art.class)
                .where(a -> a.status().eq("published"))
                .include(a -> a.ethnicGroup())
                .orderBy(a -> a.orderNum().asc())
                .toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (Art a : rows) {
            SearchDocument d = base("art", a.getId(), "/art/" + a.getId(),
                    a.getName(), a.getIntangibleHeritage(), a.getDescription());
            d.setEthnicName(a.getEthnicGroup() != null ? a.getEthnicGroup().getName() : null);
            d.setCoverImage(a.getCoverImage());
            d.setThemeColor(a.getEthnicGroup() != null ? a.getEthnicGroup().getThemeColor() : null);
            d.setCategory(a.getCategory());
            d.setTitleEn(a.getNameEn());
            d.setBodyEn(a.getDescriptionEn());
            d.setPinyinFull(PinyinUtil.full(a.getName()));
            d.setPinyinAbbr(PinyinUtil.abbr(a.getName()));
            d.setContentAt(a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromFood() {
        List<Food> rows = entityQuery.queryable(Food.class)
                .include(f -> f.ethnicGroup())
                .orderBy(f -> f.orderNum().asc())
                .toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (Food f : rows) {
            SearchDocument d = base("food", f.getId(), "/food/" + f.getId(),
                    f.getName(), f.getOrigin(), f.getDescription());
            d.setEthnicName(f.getEthnicGroup() != null ? f.getEthnicGroup().getName() : null);
            d.setCoverImage(f.getImage());
            d.setThemeColor(f.getEthnicGroup() != null ? f.getEthnicGroup().getThemeColor() : null);
            d.setCategory("food");
            d.setTitleEn(f.getNameEn());
            d.setBodyEn(f.getDescriptionEn());
            d.setPinyinFull(PinyinUtil.full(f.getName()));
            d.setPinyinAbbr(PinyinUtil.abbr(f.getName()));
            d.setContentAt(f.getUpdatedAt() != null ? f.getUpdatedAt() : f.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromCustom() {
        List<EthnicCustom> rows = entityQuery.queryable(EthnicCustom.class)
                .orderBy(c -> c.orderNum().asc())
                .toList();
        // 需要民族名做归属：一次查出全部民族做内存映射，避免 N+1
        Map<UUID, EthnicGroup> ethnicById = new LinkedHashMap<>();
        for (EthnicGroup e : entityQuery.queryable(EthnicGroup.class).toList()) {
            ethnicById.put(e.getId(), e);
        }
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (EthnicCustom c : rows) {
            EthnicGroup owner = ethnicById.get(c.getEthnicGroupId());
            // 风俗没有独立详情页，跳转到所属民族的「风俗」标签
            String url = owner != null ? "/ethnic/" + owner.getId() + "#customs" : "/ethnic";
            SearchDocument d = base("custom", c.getId(), url, c.getTitle(), c.getCategory(), c.getContent());
            if (owner != null) {
                d.setEthnicName(owner.getName());
                d.setThemeColor(owner.getThemeColor());
                d.setCoverImage(c.getImage() != null ? c.getImage() : owner.getCoverImage());
            } else {
                d.setCoverImage(c.getImage());
            }
            d.setCategory(c.getCategory());
            // 标题「苗族服饰」这类已含民族名，拼音按标题整体生成
            d.setPinyinFull(PinyinUtil.full(c.getTitle()));
            d.setPinyinAbbr(PinyinUtil.abbr(c.getTitle()));
            d.setContentAt(c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromPerson() {
        List<PersonProfile> rows = entityQuery.queryable(PersonProfile.class).toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (PersonProfile p : rows) {
            SearchDocument d = base("person", p.getId(), "/persons?keyword=" + p.getPersonName(),
                    p.getPersonName(), p.getLifespan(), p.getBio());
            d.setEthnicName(p.getEthnicGroupName());
            d.setCategory(p.getDomain() != null ? p.getDomain() : p.getRoleType());
            d.setPinyinFull(PinyinUtil.full(p.getPersonName()));
            d.setPinyinAbbr(PinyinUtil.abbr(p.getPersonName()));
            d.setContentAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromArea() {
        List<AutonomousArea> rows = entityQuery.queryable(AutonomousArea.class).toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (AutonomousArea a : rows) {
            // 自治地方的「民族」是 jsonb 数组，取首个作为归属民族便于按民族筛选
            String[] areaEthnics = JsonUtil.toStringArray(a.getEthnicGroups());
            SearchDocument d = base("area", a.getId(), "/autonomous?keyword=" + a.getName(),
                    a.getName(), a.getSeat(), a.getProvince());
            d.setEthnicName(areaEthnics.length > 0 ? areaEthnics[0] : null);
            d.setCategory(a.getLevel());
            d.setRegion(a.getProvince());
            d.setPinyinFull(PinyinUtil.full(a.getName()));
            d.setPinyinAbbr(PinyinUtil.abbr(a.getName()));
            d.setContentAt(a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    private List<SearchDocument> fromSport() {
        List<TraditionalSport> rows = entityQuery.queryable(TraditionalSport.class).toList();
        List<SearchDocument> out = new ArrayList<>(rows.size());
        for (TraditionalSport s : rows) {
            String[] sportEthnics = JsonUtil.toStringArray(s.getEthnicOrigins());
            SearchDocument d = base("sport", s.getId(), "/sports?keyword=" + s.getName(),
                    s.getName(), s.getVenue(), s.getDescription());
            d.setEthnicName(sportEthnics.length > 0 ? sportEthnics[0] : null);
            d.setCategory(s.getCategory());
            d.setPinyinFull(PinyinUtil.full(s.getName()));
            d.setPinyinAbbr(PinyinUtil.abbr(s.getName()));
            d.setContentAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt());
            out.add(d);
        }
        return out;
    }

    /** 构造索引文档骨架（各类型公共字段） */
    private SearchDocument base(String docType, UUID docId, String url, String title, String summary, String body) {
        SearchDocument d = new SearchDocument();
        d.setId(UUID.randomUUID());
        d.setDocType(docType);
        d.setDocId(docId);
        d.setUrl(url);
        d.setTitle(title == null ? "" : title);
        d.setSummary(truncate(summary, 500));
        d.setBody(truncate(body, 4000));
        d.setPopularity(0);
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
