package com.czdr.work.service.impl;

import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.entity.proxy.EthnicCustomProxy;
import com.czdr.work.model.resource.CultureTopicResource;
import com.czdr.work.service.CultureTopicService;
import com.czdr.work.service.EthnicService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文化专题聚合实现（B-1 民族服饰 / B-2 民居建筑）
 *
 * <p><b>为什么需要聚合而不是新建内容表</b>：库中这两个主题的内容已经存在，只是分散在两处 ——</p>
 * <ul>
 *   <li>{@code ethnic_custom}：风俗习惯表，其 {@code category} 已含「服饰」「居住」「建筑」，
 *       内容讲**文化特征**（如「苗族银饰盛装」「傣族竹楼」），但**无配图**；</li>
 *   <li>{@code art}：非遗项目表，其 {@code category} 含「costume」「architecture」，
 *       内容是**名录项目**（如「瑶族服饰」国家级非遗），**有专属配图**。</li>
 * </ul>
 * <p>两者按**民族**合流即可组成完整专题：以 custom 提供文化解读、以 art 提供非遗属性与配图。
 * 这样既盘活了既有数据，也避免为专题重复录入内容。</p>
 *
 * <p>聚合在内存中完成（数据量：custom 约 40 条 + art 约 28 条），
 * 与人口统计 / 非遗统计采用同一策略，不为一次专题展示引入复杂 SQL。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class CultureTopicServiceImpl implements CultureTopicService {

    /** 服饰专题的分类名（对应 ethnic_custom.category） */
    private static final String CUSTOM_CATEGORY_COSTUME = "服饰";
    /** 民居专题的分类名：居民「居住」+「建筑」两类合并 */
    private static final Set<String> CUSTOM_CATEGORIES_DWELLING = Set.of("居住", "建筑");
    /** 服饰专题对应的 art.category */
    private static final String ART_CATEGORY_COSTUME = "costume";
    /** 民居专题对应的 art.category */
    private static final String ART_CATEGORY_ARCHITECTURE = "architecture";

    private static final Map<String, String> HERITAGE_LABELS = Map.of(
            "world", "世界级",
            "national", "国家级",
            "provincial", "省级"
    );

    private final EasyEntityQuery entityQuery;
    private final EthnicService ethnicService;

    /** 专题定义：把「取哪些 custom 分类 / 哪些 art 分类」参数化，两个专题共用一套聚合逻辑 */
    private record TopicSpec(
            String code,
            String title,
            String titleEn,
            String intro,
            List<String> customCategories,
            String artCategory
    ) {
    }

    private static TopicSpec specOf(String topic) {
        if (TOPIC_COSTUME.equals(topic)) {
            return new TopicSpec(
                    TOPIC_COSTUME,
                    "民族服饰",
                    "Ethnic Costumes",
                    "服饰是民族最直观的文化标识。从苗族的银饰盛装到赫哲族的鱼皮衣，"
                            + "每一件衣裳都记录着生存环境、信仰与审美。",
                    List.of(CUSTOM_CATEGORY_COSTUME),
                    ART_CATEGORY_COSTUME
            );
        }
        if (TOPIC_DWELLING.equals(topic)) {
            return new TopicSpec(
                    TOPIC_DWELLING,
                    "民居建筑",
                    "Traditional Dwellings",
                    "建筑是凝固的生活方式。草原上的蒙古包、雨林中的竹楼、高原上的碉房，"
                            + "各民族以本地材料回应气候与地形，形成了多样的居住智慧。",
                    List.copyOf(CUSTOM_CATEGORIES_DWELLING),
                    ART_CATEGORY_ARCHITECTURE
            );
        }
        return null;
    }

    @Override
    public CultureTopicResource topic(String topic) {
        TopicSpec spec = specOf(topic);
        if (spec == null) {
            return null;
        }

        // ---- 1) 风俗习惯侧：按分类取内容 ----
        List<EthnicCustom> customs = entityQuery.queryable(EthnicCustom.class)
                .where(c -> c.category().in(spec.customCategories()))
                .toList();

        // ---- 2) 非遗项目侧：按 art.category 取项目 ----
        List<Art> arts = entityQuery.queryable(Art.class)
                .where(a -> {
                    a.status().eq("published");
                    a.category().eq(spec.artCategory());
                })
                .include(ArtProxy::ethnicGroup)
                .toList();

        // ---- 3) 民族基础信息（名称 / 主题色 / 地域 / 封面），用于合流与配图回退 ----
        Map<UUID, EthnicGroup> groupMap = new LinkedHashMap<>();
        for (EthnicGroup g : ethnicService.findAll()) {
            groupMap.put(g.getId(), g);
        }

        // ---- 4) 按民族聚合 ----
        Map<UUID, List<CultureTopicResource.Item>> byGroup = new LinkedHashMap<>();

        for (EthnicCustom c : customs) {
            if (c.getEthnicGroupId() == null) {
                continue;
            }
            byGroup.computeIfAbsent(c.getEthnicGroupId(), k -> new ArrayList<>())
                    .add(new CultureTopicResource.Item(
                            "custom",
                            c.getId() != null ? c.getId().toString() : null,
                            c.getTitle(),
                            // 存**推导后的专题分类**（而非库中粗分类「服饰」），
                            // 保证前端按分类筛选时能命中（与 categories 统计同源）
                            classify(spec.code(), c.getTitle(), c.getContent()),
                            c.getContent(),
                            null,
                            c.getImage(),
                            // custom 无独立详情页时，落到所属民族的「风俗」Tab
                            "/ethnic/" + c.getEthnicGroupId() + "#customs"
                    ));
        }

        for (Art a : arts) {
            if (a.getEthnicGroupId() == null) {
                continue;
            }
            byGroup.computeIfAbsent(a.getEthnicGroupId(), k -> new ArrayList<>())
                    .add(new CultureTopicResource.Item(
                            "art",
                            a.getId().toString(),
                            a.getName(),
                            null,
                            a.getDescription(),
                            a.getIntangibleHeritage(),
                            a.getCoverImage(),
                            "/art/" + a.getId()
                    ));
        }

        // ---- 5) 组装条目，并确定每个民族的配图 ----
        List<CultureTopicResource.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, List<CultureTopicResource.Item>> e : byGroup.entrySet()) {
            EthnicGroup g = groupMap.get(e.getKey());
            if (g == null) {
                continue;
            }
            List<CultureTopicResource.Item> items = e.getValue();
            // 配图优先级：该民族在**本专题**下的非遗配图 > 该民族非遗配图 > 民族封面
            String cover = items.stream()
                    .filter(i -> "art".equals(i.source()) && i.image() != null && !i.image().isBlank())
                    .map(CultureTopicResource.Item::image)
                    .findFirst()
                    .orElseGet(() -> firstArtCoverOf(arts, g.getId(), g.getCoverImage()));

            String[] regions = EthnicConvert.parseStringArray(g.getRegion());
            entries.add(new CultureTopicResource.Entry(
                    g.getId().toString(),
                    g.getName(),
                    g.getThemeColor(),
                    regions.length > 0 ? regions[0] : null,
                    cover,
                    items
            ));
        }

        // 排序：条目多的民族靠前，其次按民族序号
        entries.sort(Comparator
                .comparingInt((CultureTopicResource.Entry en) -> en.items().size()).reversed()
                .thenComparing(CultureTopicResource.Entry::ethnicGroupName));

        // ---- 6) 分类统计 ----
        // 服饰：按「工艺技法」归纳（从内容标题/正文关键词识别），民居：按建筑形制
        List<CultureTopicResource.Category> categories = buildCategories(spec, customs);

        int heritageCount = (int) entries.stream()
                .flatMap(en -> en.items().stream())
                .filter(i -> "art".equals(i.source()))
                .count();
        int withImage = (int) entries.stream()
                .filter(en -> en.coverImage() != null && !en.coverImage().isBlank())
                .count();

        return new CultureTopicResource(
                spec.code(),
                spec.title(),
                spec.titleEn(),
                spec.intro(),
                new CultureTopicResource.Summary(
                        entries.size(),
                        entries.stream().mapToInt(en -> en.items().size()).sum(),
                        heritageCount,
                        withImage
                ),
                categories,
                entries
        );
    }

    /** 该民族在其他专题下的非遗配图（退而求其次，避免条目无图） */
    private String firstArtCoverOf(List<Art> arts, UUID groupId, String fallback) {
        return arts.stream()
                .filter(a -> groupId.equals(a.getEthnicGroupId()))
                .map(Art::getCoverImage)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    /**
     * 专题的分类规则：关键词 → 分类名。
     * <p>库中 {@code ethnic_custom.category} 只有「服饰」「居住」「建筑」这类**粗分类**，
     * 不足以支撑专题内的细分浏览，故按内容关键词推导更贴近读者的分类
     * （服饰看工艺技法、民居看建筑形制）。规则依据库中实际内容整理。</p>
     */
    private Map<String, List<String>> classifyRules(String topic) {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        if (TOPIC_COSTUME.equals(topic)) {
            rules.put("织锦", List.of("锦", "西兰卡普"));
            rules.put("刺绣", List.of("绣"));
            rules.put("印染", List.of("蜡染", "扎染", "染"));
            rules.put("银饰", List.of("银"));
            rules.put("编织", List.of("竹帽", "编织", "藤"));
            rules.put("皮毛工艺", List.of("鱼皮", "皮"));
        } else {
            rules.put("干栏式", List.of("干栏", "吊脚楼", "竹楼", "船型屋"));
            rules.put("穹庐式", List.of("蒙古包", "毡房", "包"));
            rules.put("碉楼碉房", List.of("碉"));
            rules.put("木构", List.of("木构", "鼓楼", "风雨桥", "篱笆楼"));
            rules.put("庭院式", List.of("庭院", "土坯", "石板房", "万字炕"));
            rules.put("宗教建筑", List.of("清真寺", "经堂"));
        }
        return rules;
    }

    /**
     * 按关键词把一个条目归入分类。
     * <p><b>注意</b>：此方法同时用于「生成分类统计」与「给条目打分类标签」，
     * 两处必须走同一套规则 —— 否则前端按分类筛选时会因标签不一致而筛不出结果。</p>
     *
     * @return 命中的分类名；无命中返回「其他」
     */
    private String classify(String topic, String title, String content) {
        String text = (title == null ? "" : title) + (content == null ? "" : content);
        for (Map.Entry<String, List<String>> rule : classifyRules(topic).entrySet()) {
            if (rule.getValue().stream().anyMatch(text::contains)) {
                return rule.getKey();
            }
        }
        return "其他";
    }

    /**
     * 生成分类统计。
     * <p>与 {@link #classify} 共用同一套规则，因此统计数字与前端筛选结果**必然一致** ——
     * 这里曾出过一个 bug：统计按规则推导、条目却仍用库中的粗分类（「服饰」），
     * 导致点分类筛选时筛不出任何结果。现统一口径。</p>
     */
    private List<CultureTopicResource.Category> buildCategories(
            TopicSpec spec, List<EthnicCustom> customs) {

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String label : classifyRules(spec.code()).keySet()) {
            counts.put(label, 0);
        }
        counts.put("其他", 0);

        for (EthnicCustom c : customs) {
            counts.merge(classify(spec.code(), c.getTitle(), c.getContent()), 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new CultureTopicResource.Category(e.getKey(), e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(CultureTopicResource.Category::count).reversed())
                .toList();
    }
}
