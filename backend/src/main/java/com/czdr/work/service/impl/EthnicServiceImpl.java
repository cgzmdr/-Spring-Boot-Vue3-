package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.EthnicGroupQueryBind;
import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.entity.proxy.EthnicCustomProxy;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
import com.czdr.work.model.entity.proxy.FoodProxy;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicMapPointResource;
import com.czdr.work.model.resource.EthnicPopulationStatsResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.czdr.work.service.EthnicService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class EthnicServiceImpl implements EthnicService {

    /** 七普口径（population 字段的统计年份） */
    private static final String CENSUS_YEAR = "2020";

    private final EasyEntityQuery entityQuery;
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
        List<EthnicQueryInfoResource> data = pageResult.getData().stream()
                .map(EthnicConvert::toInfoModel)
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
     */
    @Override
    public EthnicPopulationStatsResource findPopulationStats(int topN) {
        List<EthnicGroup> groups = findAll();
        int limit = topN > 0 ? Math.min(topN, groups.size()) : groups.size();

        List<EthnicGroup> sorted = groups.stream()
                .sorted(Comparator.comparingLong(EthnicGroup::getPopulation).reversed())
                .toList();

        long totalPopulation = sorted.stream().mapToLong(EthnicGroup::getPopulation).sum();
        EthnicGroup largest = sorted.isEmpty() ? null : sorted.get(0);

        List<EthnicPopulationStatsResource.Item> topGroups = sorted.stream()
                .limit(limit)
                .map(g -> new EthnicPopulationStatsResource.Item(
                        g.getName(), 1, g.getPopulation()))
                .toList();

        return new EthnicPopulationStatsResource(
                CENSUS_YEAR,
                sorted.size(),
                totalPopulation,
                largest != null ? largest.getName() : null,
                largest != null ? largest.getPopulation() : 0L,
                buildBuckets(sorted),
                topGroups,
                aggregate(sorted, g -> List.of(blankToOther(g.getLanguageFamily()))),
                aggregate(sorted, g -> EthnicConvert.parseStringList(g.getRegion()))
        );
    }

    /**
     * 按给定维度聚合人口。
     * <p>一个民族可能命中多个键（多地域），此时该民族会分别计入每个地域；
     * 因此地域维度的 {@code population} 合计会大于全国民族人口总数，属于预期行为。</p>
     */
    private List<EthnicPopulationStatsResource.Item> aggregate(
            List<EthnicGroup> groups, java.util.function.Function<EthnicGroup, List<String>> keyFn) {
        Map<String, long[]> acc = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EthnicGroup g : groups) {
            List<String> keys = keyFn.apply(g);
            if (keys == null || keys.isEmpty()) {
                keys = List.of("其他");
            }
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                acc.computeIfAbsent(key, k -> new long[1])[0] += g.getPopulation();
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
    private List<EthnicPopulationStatsResource.Bucket> buildBuckets(List<EthnicGroup> sorted) {
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
            List<EthnicGroup> hit = sorted.stream()
                    .filter(g -> g.getPopulation() >= min && g.getPopulation() < max)
                    .toList();
            buckets.add(new EthnicPopulationStatsResource.Bucket(
                    labels[i],
                    hit.size(),
                    hit.stream().mapToLong(EthnicGroup::getPopulation).sum()));
        }
        return buckets;
    }

    private String blankToOther(String value) {
        return value == null || value.isBlank() ? "未分类" : value;
    }
}