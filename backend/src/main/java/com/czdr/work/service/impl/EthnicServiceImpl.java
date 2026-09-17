package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.EthnicGroupQueryBind;
import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.proxy.EthnicCustomProxy;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
import com.czdr.work.model.entity.proxy.FoodProxy;
import com.czdr.work.model.more.BasicMetrics;
import com.czdr.work.model.more.EthnicLightRow;
import com.czdr.work.model.more.EthnicRelated;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicMapPointResource;
import com.czdr.work.model.resource.EthnicPopulationStatsResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.czdr.work.service.EthnicService;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
/**
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthnicServiceImpl implements EthnicService {

    /** 七普口径（population 字段的统计年份） */
    private static final String CENSUS_YEAR = "2020";

    private final EasyEntityQuery entityQuery;
    private final com.czdr.work.service.EthnicMetricsService ethnicMetricsService;
    private final com.czdr.work.config.ReadCache readCache;

    @Override
    public EasyPageResult<EthnicQueryInfoResource> find(EthnicQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<EthnicGroupProxy>> binds = new EthnicGroupQueryBind(request).customize();
        Map<String, Object> map = request.toMap();
        Sort sort = pageable.getSort();
        EasyPageResult<EthnicGroup> pageResult = entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.status().eq("published");
                    map.forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<EthnicGroupProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(ethnicGroupProxy);
                            }
                        }
                    });
                })
                .orderBy(t -> {
                    // 遍历 Spring 的 Sort 对象
                    for (Sort.Order order : sort) {
                        String property = order.getProperty();
                        boolean isAsc = order.isAscending();
                        switch (property) {
                            case "id":
                                t.id().orderBy(isAsc);
                                break;
                            case "name":
                                t.name().orderBy(isAsc);
                                break;
                            case "population":
                                t.population().orderBy(isAsc);
                                break;
                            case "pinyin":
                                t.pinyin().orderBy(isAsc);
                                break;
                            // 如果有更多字段，继续添加 case...
                            default:
                                // 可选：记录日志或抛出异常，防止前端传入非法字段
                                break;
                        }
                    }
                })
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        // 关联指标（非遗/节日/美食/风俗/聚居地/人物/自治地方/人口排名）：
        // 一次性取全量指标后按 ID 查表，避免逐条查询。
        Map<UUID, BasicMetrics> metrics = ethnicMetricsService.loadAll();

        List<EthnicQueryInfoResource> data = pageResult.getData().stream()
                .map(e -> EthnicConvert.toInfoModel(e, metrics.get(e.getId())))
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public EthnicGroup find(String id) {
        UUID uuid = UUID.fromString(id);
        return entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.id().eq(uuid);
                })
                .include(EthnicGroupProxy::customs)
                .include(EthnicGroupProxy::locations)
                .include(EthnicGroupProxy::foods)
                .include(EthnicGroupProxy::festivals)
                .include(EthnicGroupProxy::arts)
                .firstNotNull();
    }

    @Override
    public EthnicCustom findCustom(String id) {
        UUID uuid = UUID.fromString(id);
        EthnicCustom custom = entityQuery.queryable(EthnicCustom.class)
                .where(customProxy -> {
                    customProxy.id().eq(uuid);
                })
                .include(EthnicCustomProxy::ethnicGroup)
                .firstOrNull();
        if (custom == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return custom;
    }
    @Override
    public Food findFood(String id) {
        UUID uuid = UUID.fromString(id);
        Food food = entityQuery.queryable(Food.class)
                .where(foodProxy -> {
                    foodProxy.id().eq(uuid);
                })
                .include(FoodProxy::ethnicGroup)
                .firstOrNull();
        if (food == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return food;
    }

    @Override
    public List<EthnicGroup> findAll() {
        return entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.status().eq("published");
                })
                .orderBy(EthnicGroupProxy::orderNum)
                .toList();
    }

    /**
     * 轻量版全量查询：只取人口统计所需的字段（ID / 名称 / 人口 / 语系 / 地域）。
     *
     * <p><b>为什么不直接复用 {@link #findAll()}</b>：后者返回完整实体，
     * 其中 {@code description}（民族简介正文）+ {@code description_en} 在 56 行里
     * 合计约 600KB，而人口统计只需算总数、分档与按语系/地域聚合。
     * 取全实体等于每次请求都白白传输并反序列化这些正文。</p>
     *
     * <p><b>实现取舍</b>：用「每列一条单列 select」而不是 DTO 投影 ——
     * 本项目既有的 EasyQuery 用法以单列 {@code select(proxy -> proxy.column())} 为主，
     * 行为可预期；DTO 投影需要为 DTO 生成额外 Proxy 类，配置成本更高。
     * 5 条短列查询的数据量远小于一次全实体查询（含 600KB 正文）。</p>
     *
     * <p>⚠️ 五条查询使用<b>完全相同的排序</b>（orderNum, id），
     * 保证结果可按相同下标对齐。</p>
     */
    private List<EthnicLightRow> findLightweight() {
        List<UUID> ids = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc()).orderBy(g -> g.id().asc())
                .select(g -> g.id()).toList();
        List<String> names = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc()).orderBy(g -> g.id().asc())
                .select(g -> g.name()).toList();
        List<Long> populations = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc()).orderBy(g -> g.id().asc())
                .select(g -> g.population()).toList();
        List<String> families = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc()).orderBy(g -> g.id().asc())
                .select(g -> g.languageFamily()).toList();
        List<String> regions = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc()).orderBy(g -> g.id().asc())
                .select(g -> g.region()).toList();

        int n = Math.min(ids.size(), Math.min(names.size(), Math.min(populations.size(),
                Math.min(families.size(), regions.size()))));
        List<EthnicLightRow> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new EthnicLightRow(
                    ids.get(i), names.get(i), populations.get(i), families.get(i), regions.get(i)));
        }
        return out;
    }

    /**
     * 民族分布地图点位。
     * <p>以聚居地为主体、民族为附属：一次性把已发布民族的聚居地查出来（含民族导航属性），
     * 过滤掉经纬度缺失的记录。112 条聚居地规模很小，全部返回即可，无需分页。</p>
     */
    @Override
    public List<EthnicMapPointResource> findMapPoints(String ethnicGroupId) {
        UUID filterId = null;
        if (ethnicGroupId != null && !ethnicGroupId.isBlank()) {
            try {
                filterId = UUID.fromString(ethnicGroupId);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "民族 ID 格式不正确");
            }
        }
        UUID targetId = filterId;

        List<EthnicGroup> groups = entityQuery.queryable(EthnicGroup.class)
                .where(g -> {
                    g.status().eq("published");
                    if (targetId != null) {
                        g.id().eq(targetId);
                    }
                })
                .orderBy(EthnicGroupProxy::orderNum)
                .include(EthnicGroupProxy::locations)
                .toList();

        List<EthnicMapPointResource> points = new ArrayList<>();
        for (EthnicGroup group : groups) {
            if (group.getLocations() == null) {
                continue;
            }
            group.getLocations().stream()
                    .filter(loc -> loc.getLongitude() != null && loc.getLatitude() != null)
                    .sorted(Comparator.comparing(loc -> loc.getOrderNum() == null ? Integer.MAX_VALUE : loc.getOrderNum()))
                    .forEach(loc -> points.add(new EthnicMapPointResource(
                            loc.getId() != null ? loc.getId().toString() : null,
                            group.getId().toString(),
                            group.getName(),
                            group.getSlug(),
                            group.getThemeColor(),
                            loc.getProvince(),
                            loc.getCity(),
                            loc.getLongitude().doubleValue(),
                            loc.getLatitude().doubleValue(),
                            loc.getDescription()
                    )));
        }
        return points;
    }

    /**
     * 民族人口统计（七普口径）。
     * <p>只做展示层的轻量聚合：数据量固定为 56 条，直接在内存中分组统计，
     * 避免为一次首页可视化引入复杂的 SQL 分组（含 jsonb 数组展开）。</p>
     *
     * <p><b>性能</b>：这里刻意<b>不</b>用 {@link #findAll()} —— 那会取 ethnic_group 的全部列，
     * 而该表正文列 {@code description}（民族简介）就有约 600KB / 56 行，
     * 人口统计完全用不到。改为只取聚合所需的四个字段（名称/人口/语系/地域），
     * 把该接口的单次响应数据量从「全表实体」降到「几十 KB 的小字段」。</p>
     */
    @Override
    public EthnicPopulationStatsResource findPopulationStats(int topN) {
        // 人口统计是「全量聚合 + 极少变化」的典型只读数据（民族人口每年才更新一次），
        // 因此套用项目既有的 ReadCache（TTL 默认 60s，内容后台变更可主动失效）。
        // 注意：缓存键必须带上 topN —— 不同 topN 的结果不同。
        return readCache.get("ethnic:population-stats:" + topN, () -> computePopulationStats(topN));
    }

    private EthnicPopulationStatsResource computePopulationStats(int topN) {
        List<EthnicLightRow> groups = findLightweight();
        int limit = topN > 0 ? Math.min(topN, groups.size()) : groups.size();

        List<EthnicLightRow> sorted = groups.stream()
                .sorted(Comparator.comparingLong(EthnicLightRow::population).reversed())
                .toList();

        long totalPopulation = sorted.stream().mapToLong(EthnicLightRow::population).sum();
        EthnicLightRow largest = sorted.isEmpty() ? null : sorted.get(0);

        List<EthnicPopulationStatsResource.Item> topGroups = sorted.stream()
                .limit(limit)
                .map(g -> new EthnicPopulationStatsResource.Item(
                        g.name(), 1, g.population()))
                .toList();

        return new EthnicPopulationStatsResource(
                CENSUS_YEAR,
                sorted.size(),
                totalPopulation,
                largest != null ? largest.name() : null,
                largest != null ? largest.population() : 0L,
                buildBuckets(sorted),
                topGroups,
                aggregate(sorted, g -> List.of(blankToOther(g.languageFamily()))),
                aggregate(sorted, g -> EthnicConvert.parseStringList(g.region()))
        );
    }

    /**
     * 按给定维度聚合人口。
     * <p>一个民族可能命中多个键（多地域），此时该民族会分别计入每个地域；
     * 因此地域维度的 {@code population} 合计会大于全国民族人口总数，属于预期行为。</p>
     */
    private List<EthnicPopulationStatsResource.Item> aggregate(
            List<EthnicLightRow> groups, java.util.function.Function<EthnicLightRow, List<String>> keyFn) {
        Map<String, long[]> acc = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EthnicLightRow g : groups) {
            List<String> keys = keyFn.apply(g);
            if (keys == null || keys.isEmpty()) {
                keys = List.of("其他");
            }
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                acc.computeIfAbsent(key, k -> new long[1])[0] += g.population();
                counts.merge(key, 1, Integer::sum);
            }
        }
        return acc.entrySet().stream()
                .map(e -> new EthnicPopulationStatsResource.Item(
                        e.getKey(), counts.getOrDefault(e.getKey(), 0), e.getValue()[0]))
                .sorted(Comparator.comparingLong(EthnicPopulationStatsResource.Item::population).reversed())
                .toList();
    }

    /** 人口规模分档：固定 5 档，便于前端直接画柱状图 / 环形图 */
    private List<EthnicPopulationStatsResource.Bucket> buildBuckets(List<EthnicLightRow> sorted) {
        long[][] thresholds = {
                {10_000_000L, Long.MAX_VALUE},
                {1_000_000L, 10_000_000L},
                {100_000L, 1_000_000L},
                {10_000L, 100_000L},
                {0L, 10_000L}
        };
        String[] labels = {"1000 万以上", "100 万 – 1000 万", "10 万 – 100 万", "1 万 – 10 万", "1 万以下"};

        List<EthnicPopulationStatsResource.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < thresholds.length; i++) {
            long min = thresholds[i][0];
            long max = thresholds[i][1];
            List<EthnicLightRow> hit = sorted.stream()
                    .filter(g -> g.population() >= min && g.population() < max)
                    .toList();
            buckets.add(new EthnicPopulationStatsResource.Bucket(
                    labels[i],
                    hit.size(),
                    hit.stream().mapToLong(EthnicLightRow::population).sum()));
        }
        return buckets;
    }

    private String blankToOther(String value) {
        return value == null || value.isBlank() ? "未分类" : value;
    }

    // ==================================================================================
    // 详情关联信息（信息量增强）
    // ==================================================================================

    /**
     * 组装民族详情的关联信息。
     *
     * <p>两个关联维度都不是外键，而是按「名称」匹配，因此这里显式说明依据：</p>
     * <ul>
     *   <li><b>人物</b>：{@code person_profile.ethnic_group_name} 存的是民族名（字符串），
     *       与该民族 {@code ethnic_group.name} 精确匹配；</li>
     *   <li><b>自治地方</b>：{@code autonomous_area.ethnic_groups} 是 JSON 数组
     *       （如 {@code ["蒙古族"]}），解析后包含该民族名即计入。</li>
     * </ul>
     *
     * <p>匹配不到时返回空列表，不臆造关联。</p>
     */
    @Override
    public EthnicRelated relatedOf(EthnicGroup ethnicGroup) {
        if (ethnicGroup == null || ethnicGroup.getName() == null) {
            return EthnicRelated.EMPTY;
        }
        String name = ethnicGroup.getName().trim();

        // ---------- 关联人物 ----------
        List<PersonProfile> persons = List.of();
        try {
            persons = entityQuery.queryable(PersonProfile.class)
                    .where(p -> p.ethnicGroupName().eq(name))
                    .toList();
        } catch (Exception e) {
            log.warn("查询民族关联人物失败（按空处理）: {}", e.getMessage());
        }

        // ---------- 自治地方（JSON 数组含该民族名） ----------
        List<AutonomousArea> areas = List.of();
        try {
            areas = entityQuery.queryable(AutonomousArea.class).toList().stream()
                    .filter(a -> {
                        for (String g : JsonUtil.toStringArray(a.getEthnicGroups())) {
                            if (g != null && name.equals(g.trim())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    // 级别排序：自治区 > 自治州 > 自治县·旗，便于前端按重要性呈现
                    .sorted(Comparator.comparingInt(a -> levelOrder(a.getLevel())))
                    .toList();
        } catch (Exception e) {
            log.warn("查询民族关联自治地方失败（按空处理）: {}", e.getMessage());
        }

        // ---------- 人口排名 ----------
        int rank = 0;
        int total = 0;
        try {
            List<EthnicGroup> all = new ArrayList<>(entityQuery.queryable(EthnicGroup.class)
                    .where(g -> g.status().eq("published"))
                    .toList());
            total = all.size();
            all.sort(Comparator.comparingLong(EthnicGroup::getPopulation).reversed());
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(ethnicGroup.getId())) {
                    rank = i + 1;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("计算民族人口排名失败（按 0 处理）: {}", e.getMessage());
        }

        return new EthnicRelated(persons, areas, rank, total);
    }

    /** 自治地方级别排序权重（自治区最前） */
    private int levelOrder(String level) {
        if (level == null) {
            return 9;
        }
        return switch (level) {
            case "autonomous_region" -> 0;
            case "autonomous_prefecture" -> 1;
            case "autonomous_county" -> 2;
            default -> 9;
        };
    }
}