package com.czdr.work.service.impl;

import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.resource.AutonomousAreaResource;
import com.czdr.work.service.AutonomousAreaService;
import com.czdr.work.service.EthnicService;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 民族自治地方实现（B-6）
 *
 * <p>155 条行政区划数据量很小，一次性取出后在内存中分组与聚合，
 * 与人口统计 / 非遗统计 / 文化专题采用同一策略。</p>
 *
 * <p><b>与内容库的关联</b>：自治地方的自治民族名称（如「朝鲜族」）可与
 * {@code ethnic_group.name} 匹配，从而在页面上直接跳转到该民族的详情页。
 * 匹配是**尽力而为**的 —— 内容库只收录 56 个民族，而自治地方涉及的民族更多
 * （如「土家族」「满族」在库中，但某些自治县的冠名民族可能不在），
 * 匹配不上时前端只展示名称、不提供跳转。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class AutonomousAreaServiceImpl implements AutonomousAreaService {

    private static final Map<String, String> LEVEL_LABELS = Map.of(
            "autonomous_region", "自治区",
            "autonomous_prefecture", "自治州",
            "autonomous_county", "自治县 / 自治旗"
    );

    /** 级别展示顺序：自治区 → 自治州 → 自治县 */
    private static final List<String> LEVEL_ORDER = List.of(
            "autonomous_region", "autonomous_prefecture", "autonomous_county"
    );

    private final EasyEntityQuery entityQuery;
    private final EthnicService ethnicService;

    @Override
    public AutonomousAreaResource directory(String level, String keyword, String ethnic) {
        List<AutonomousArea> all = entityQuery.queryable(AutonomousArea.class)
                .orderBy(a -> a.name().asc())
                .toList();

        // 内容库民族索引：名称 -> 民族（用于生成跳转链接）
        Map<String, EthnicGroup> libraryByName = new LinkedHashMap<>();
        for (EthnicGroup g : ethnicService.findAll()) {
            libraryByName.put(g.getName(), g);
        }

        String kw = keyword == null ? "" : keyword.trim();

        // ---- 按级别分组（同时应用筛选）----
        List<AutonomousAreaResource.LevelGroup> levels = new ArrayList<>();
        for (String lv : LEVEL_ORDER) {
            List<AutonomousAreaResource.Area> areas = all.stream()
                    .filter(a -> lv.equals(a.getLevel()))
                    .filter(a -> level == null || level.isBlank() || level.equals(a.getLevel()))
                    .filter(a -> kw.isEmpty() || (a.getName() != null && a.getName().contains(kw)))
                    .filter(a -> ethnic == null || ethnic.isBlank() || toAreasMatchesEthnic(a, ethnic))
                    .map(this::toArea)
                    .toList();
            if (!areas.isEmpty()) {
                levels.add(new AutonomousAreaResource.LevelGroup(
                        lv, LEVEL_LABELS.getOrDefault(lv, lv), areas.size(), areas));
            }
        }

        // ---- 按自治民族聚合 ----
        // 注意：一个自治地方可能冠名多个民族（如「双江拉祜族佤族布朗族傣族自治县」），
        // 该地方会分别计入每个民族，因此各类计数之和 > 155，属预期行为。
        Map<String, List<AutonomousArea>> byEthnic = new LinkedHashMap<>();
        for (AutonomousArea a : all) {
            for (String e : JsonUtil.toStringArray(a.getEthnicGroups())) {
                if (e != null && !e.isBlank()) {
                    byEthnic.computeIfAbsent(e.trim(), k -> new ArrayList<>()).add(a);
                }
            }
        }
        List<AutonomousAreaResource.EthnicGrouping> ethnics = byEthnic.entrySet().stream()
                .map(en -> {
                    EthnicGroup lib = libraryByName.get(en.getKey());
                    return new AutonomousAreaResource.EthnicGrouping(
                            en.getKey(),
                            en.getValue().size(),
                            lib != null ? lib.getName() : null,
                            lib != null ? lib.getSlug() : null,
                            lib != null ? lib.getThemeColor() : null,
                            en.getValue().stream().map(this::toArea).toList()
                    );
                })
                .sorted(Comparator
                        .comparingInt(AutonomousAreaResource.EthnicGrouping::count).reversed()
                        .thenComparing(AutonomousAreaResource.EthnicGrouping::ethnic))
                .toList();

        // ---- 按省级行政区聚合 ----
        Map<String, List<AutonomousArea>> byProvince = new LinkedHashMap<>();
        for (AutonomousArea a : all) {
            String p = a.getProvince();
            if (p == null || p.isBlank()) {
                continue;
            }
            byProvince.computeIfAbsent(p, k -> new ArrayList<>()).add(a);
        }
        List<AutonomousAreaResource.ProvinceGrouping> provinces = byProvince.entrySet().stream()
                .map(en -> new AutonomousAreaResource.ProvinceGrouping(
                        en.getKey(),
                        en.getValue().size(),
                        en.getValue().stream()
                                // 自治区自身不再作为下级重复列出
                                .filter(a -> !"autonomous_region".equals(a.getLevel()))
                                .map(this::toArea)
                                .toList()
                ))
                .sorted(Comparator
                        .comparingInt(AutonomousAreaResource.ProvinceGrouping::count).reversed()
                        .thenComparing(AutonomousAreaResource.ProvinceGrouping::province))
                .toList();

        AutonomousAreaResource.Summary summary = new AutonomousAreaResource.Summary(
                all.size(),
                (int) all.stream().filter(a -> "autonomous_region".equals(a.getLevel())).count(),
                (int) all.stream().filter(a -> "autonomous_prefecture".equals(a.getLevel())).count(),
                (int) all.stream().filter(a -> "autonomous_county".equals(a.getLevel())).count(),
                byEthnic.size(),
                byProvince.size()
        );

        return new AutonomousAreaResource(summary, levels, ethnics, provinces);
    }

    /** 判断某自治地方是否冠名了指定民族 */
    private boolean toAreasMatchesEthnic(AutonomousArea a, String ethnic) {
        return java.util.Arrays.asList(JsonUtil.toStringArray(a.getEthnicGroups())).contains(ethnic);
    }

    private AutonomousAreaResource.Area toArea(AutonomousArea a) {
        return new AutonomousAreaResource.Area(
                a.getName(),
                a.getLevel(),
                LEVEL_LABELS.getOrDefault(a.getLevel(), a.getLevel()),
                java.util.Arrays.asList(JsonUtil.toStringArray(a.getEthnicGroups())),
                a.getProvince(),
                a.getEstablishedYear(),
                a.getSeat()
        );
    }
}
