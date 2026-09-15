package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.ArtQueryBind;
import com.czdr.work.comment.convert.ArtConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.model.resource.HeritageDirectoryStatsResource;
import com.czdr.work.service.ArtService;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
public class ArtServiceImpl implements ArtService {

    /** 非遗级别展示名（数据中仅存在 world / national 两档） */
    private static final Map<String, String> LEVEL_LABELS = Map.of(
            "world", "世界级",
            "national", "国家级",
            "provincial", "省级"
    );

    /** 类别展示名，与前端 artCategoryLabel 保持一致 */
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "music", "音乐",
            "dance", "舞蹈",
            "drama", "戏剧",
            "costume", "服饰",
            "craft", "手工艺",
            "architecture", "建筑",
            "fine_art", "美术"
    );

    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<ArtQueryInfoResource> find(ArtQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<ArtProxy>> binds = new ArtQueryBind(request).customize();
        EasyPageResult<Art> pageResult = entityQuery.queryable(Art.class)
                .where(art -> {
                    art.status().eq("published");
                    request.toMap().forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<ArtProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(art);
                            }
                        }
                    });
                })
                .include(ArtProxy::ethnicGroup)
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<ArtQueryInfoResource> data = pageResult.getData().stream()
                .map(ArtConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public Art find(String id) {
        UUID uuid = UUID.fromString(id);
        Art art = entityQuery.queryable(Art.class)
                .where(a -> {
                    a.id().eq(uuid);
                    a.status().eq("published");
                })
                .include(ArtProxy::ethnicGroup)
                .firstOrNull();
        if (art == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return art;
    }

    /**
     * 非遗名录统计概览。
     * <p>仅 165 条数据，一次性取出后在内存中分组（与人口统计同一策略），
     * 避免为一次概览引入多条 SQL 分组查询。</p>
     */
    @Override
    public HeritageDirectoryStatsResource heritageStats() {
        List<Art> all = entityQuery.queryable(Art.class)
                .where(a -> a.status().eq("published"))
                .include(ArtProxy::ethnicGroup)
                .toList();

        // 级别分布（固定顺序：世界级 → 国家级 → 省级）
        Map<String, Integer> levelCount = new LinkedHashMap<>();
        for (String code : List.of("world", "national", "provincial")) {
            levelCount.put(code, 0);
        }
        for (Art a : all) {
            String lv = a.getIntangibleHeritage();
            if (lv != null && !lv.isBlank()) {
                levelCount.merge(lv, 1, Integer::sum);
            }
        }
        List<HeritageDirectoryStatsResource.Group> levels = levelCount.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new HeritageDirectoryStatsResource.Group(
                        e.getKey(),
                        LEVEL_LABELS.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue()))
                .toList();

        // 类别分布（按数量降序）
        Map<String, Integer> catCount = new LinkedHashMap<>();
        for (Art a : all) {
            String cat = a.getCategory();
            if (cat != null && !cat.isBlank()) {
                catCount.merge(cat, 1, Integer::sum);
            }
        }
        List<HeritageDirectoryStatsResource.Group> categories = catCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new HeritageDirectoryStatsResource.Group(
                        e.getKey(),
                        CATEGORY_LABELS.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue()))
                .toList();

        // 传承人覆盖情况 + 聚焦榜（人数降序，取前 12）
        List<Art> withInheritor = all.stream()
                .filter(a -> {
                    String[] arr = JsonUtil.toStringArray(a.getInheritors());
                    return arr.length > 0;
                })
                .sorted(Comparator.comparingInt(
                        (Art a) -> JsonUtil.toStringArray(a.getInheritors()).length).reversed())
                .toList();

        List<HeritageDirectoryStatsResource.InheritorSpotlight> spotlight = new ArrayList<>();
        for (Art a : withInheritor.stream().limit(12).toList()) {
            spotlight.add(new HeritageDirectoryStatsResource.InheritorSpotlight(
                    a.getId().toString(),
                    a.getName(),
                    a.getIntangibleHeritage(),
                    a.getEthnicGroup() != null ? a.getEthnicGroup().getName() : null,
                    List.of(JsonUtil.toStringArray(a.getInheritors()))
            ));
        }

        long ethnicCount = all.stream()
                .filter(a -> a.getEthnicGroup() != null)
                .map(a -> a.getEthnicGroup().getId())
                .distinct()
                .count();

        return new HeritageDirectoryStatsResource(
                all.size(),
                withInheritor.size(),
                (int) ethnicCount,
                levels,
                categories,
                spotlight
        );
    }
}
