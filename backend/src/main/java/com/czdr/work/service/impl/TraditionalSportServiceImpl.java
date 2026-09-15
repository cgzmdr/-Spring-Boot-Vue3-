package com.czdr.work.service.impl;

import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.TraditionalSport;
import com.czdr.work.model.resource.TraditionalSportResource;
import com.czdr.work.service.EthnicService;
import com.czdr.work.service.TraditionalSportService;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 传统体育实现（B-5）
 *
 * <p>项目数量少（约 20 项），一次性取出后在内存中筛选与统计，
 * 与人口统计 / 非遗统计 / 文化专题 / 自治地方采用同一策略。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class TraditionalSportServiceImpl implements TraditionalSportService {

    /** 类别展示名，顺序即前端展示顺序（球类 → 水上 → 力量 → 技巧 → 竞速 → 武术 → 马术 → 健身操 → 秋千） */
    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>(Map.of(
            "ball", "球类",
            "water", "水上",
            "strength", "力量对抗",
            "accuracy", "射击与技巧",
            "speed", "竞速",
            "martial", "武术",
            "equestrian", "马术",
            "gymnastics", "健身操",
            "swing", "秋千"
    ));

    private static final List<String> CATEGORY_ORDER = List.of(
            "ball", "water", "strength", "accuracy", "speed",
            "martial", "equestrian", "gymnastics", "swing"
    );

    private final EasyEntityQuery entityQuery;
    private final EthnicService ethnicService;

    @Override
    public TraditionalSportResource directory(String category, String ethnic, String keyword) {
        List<TraditionalSport> all = entityQuery.queryable(TraditionalSport.class).toList();

        // 内容库民族索引（用于生成跳转）
        Map<String, EthnicGroup> libByName = new LinkedHashMap<>();
        for (EthnicGroup g : ethnicService.findAll()) {
            libByName.put(g.getName(), g);
        }

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        List<TraditionalSportResource.Sport> sports = all.stream()
                .filter(s -> category == null || category.isBlank() || category.equals(s.getCategory()))
                .filter(s -> {
                    if (ethnic == null || ethnic.isBlank()) {
                        return true;
                    }
                    return Arrays.asList(JsonUtil.toStringArray(s.getEthnicOrigins())).contains(ethnic);
                })
                .filter(s -> {
                    if (kw.isEmpty()) {
                        return true;
                    }
                    return (s.getName() != null && s.getName().toLowerCase().contains(kw))
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(kw));
                })
                .map(s -> {
                    List<TraditionalSportResource.EthnicRef> refs = new ArrayList<>();
                    for (String e : JsonUtil.toStringArray(s.getEthnicOrigins())) {
                        EthnicGroup g = libByName.get(e);
                        if (g != null) {
                            refs.add(new TraditionalSportResource.EthnicRef(
                                    g.getId().toString(), g.getName(), g.getSlug(), g.getThemeColor()));
                        }
                    }
                    return new TraditionalSportResource.Sport(
                            s.getName(),
                            s.getCategory(),
                            CATEGORY_LABELS.getOrDefault(s.getCategory(), s.getCategory()),
                            Arrays.asList(JsonUtil.toStringArray(s.getEthnicOrigins())),
                            s.getDescription(),
                            s.getEquipment(),
                            s.getVenue(),
                            s.getTeamSize(),
                            s.getFirstEventYear(),
                            Arrays.asList(JsonUtil.toStringArray(s.getSubEvents())),
                            s.getHeritageLink(),
                            refs
                    );
                })
                // 排序：按类别固定顺序 → 项目名
                .sorted(Comparator
                        .comparingInt((TraditionalSportResource.Sport sp) -> {
                            int i = CATEGORY_ORDER.indexOf(sp.category());
                            return i < 0 ? 99 : i;
                        })
                        .thenComparing(TraditionalSportResource.Sport::name))
                .toList();

        // 统计基于**全量**（不受筛选影响），便于前端展示筛选计数
        long ethnicCount = all.stream()
                .flatMap(s -> Arrays.stream(JsonUtil.toStringArray(s.getEthnicOrigins())))
                .distinct()
                .count();

        TraditionalSportResource.Summary summary = new TraditionalSportResource.Summary(
                all.size(),
                (int) all.stream().map(TraditionalSport::getCategory).distinct().count(),
                (int) ethnicCount,
                (int) all.stream().filter(s -> JsonUtil.toStringArray(s.getSubEvents()).length > 0).count(),
                (int) all.stream().filter(s -> s.getHeritageLink() != null && !s.getHeritageLink().isBlank()).count()
        );

        return new TraditionalSportResource(summary, sports);
    }
}
