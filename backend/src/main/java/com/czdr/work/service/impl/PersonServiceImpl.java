package com.czdr.work.service.impl;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.resource.PersonDirectoryResource;
import com.czdr.work.service.PersonService;
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
 * 人物专栏实现（B-7）
 *
 * <p><b>数据来源与关联方式</b>：人物姓名存在 {@code art.inheritors}（JSONB 数组），
 * 人物扩展属性存在 {@code person_profile}。两者以
 * {@code (person_name, ethnic_group_name)} 关联 ——
 * 之所以带民族，是因为存在**同名不同族**的真实情况
 * （如「马金山」同时是回族花儿与东乡族花儿的代表性传承人），
 * 仅按姓名会错误合并两个人物。</p>
 *
 * <p><b>为什么不用 SQL JOIN</b>：{@code inheritors} 是 JSONB 数组，
 * 展开后还要与 person_profile 做「姓名 + 民族」双键匹配，SQL 会相当绕且难以复用；
 * 而数据量很小（164 条人物档案 + 165 条项目），在内存中建索引后匹配更直观、更好维护。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    /** 角色类型 */
    private static final String ROLE_INHERITOR = "inheritor";
    private static final String ROLE_MASTER = "master";

    private static final Map<String, String> ROLE_LABELS = Map.of(
            ROLE_INHERITOR, "代表性传承人",
            ROLE_MASTER, "历史文化名家"
    );

    /** 非遗级别排序权重（世界级优先） */
    private static final Map<String, Integer> LEVEL_WEIGHT = Map.of(
            "world", 0,
            "national", 1,
            "provincial", 2
    );

    private final EasyEntityQuery entityQuery;

    @Override
    public PersonDirectoryResource directory(String keyword, String domain, String ethnic, String roleType) {
        // ---- 1) 人物档案 ----
        List<PersonProfile> profiles = entityQuery.queryable(PersonProfile.class).toList();

        // ---- 2) 非遗项目（用于反查每个人物关联的项目）----
        List<Art> arts = entityQuery.queryable(Art.class)
                .where(a -> a.status().eq("published"))
                .include(ArtProxy::ethnicGroup)
                .toList();

        // ---- 3) 建立「姓名 + 民族」-> 项目列表 的索引 ----
        Map<String, List<Art>> projectsByPerson = new LinkedHashMap<>();
        for (Art a : arts) {
            String[] inheritors = JsonUtil.toStringArray(a.getInheritors());
            if (inheritors.length == 0) {
                continue;
            }
            String ethnicName = a.getEthnicGroup() != null ? a.getEthnicGroup().getName() : "";
            for (String name : inheritors) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                projectsByPerson
                        .computeIfAbsent(key(name, ethnicName), k -> new ArrayList<>())
                        .add(a);
            }
        }

        // ---- 4) 组装人物列表项 ----
        List<PersonDirectoryResource.PersonItem> all = new ArrayList<>();
        for (PersonProfile p : profiles) {
            List<Art> mine = projectsByPerson.getOrDefault(
                    key(p.getPersonName(), p.getEthnicGroupName()), List.of());

            List<PersonDirectoryResource.ProjectRef> refs = mine.stream()
                    .map(a -> new PersonDirectoryResource.ProjectRef(
                            a.getId().toString(),
                            a.getName(),
                            a.getIntangibleHeritage(),
                            a.getEthnicGroup() != null ? a.getEthnicGroup().getName() : null,
                            "/art/" + a.getId()
                    ))
                    .sorted(Comparator.comparingInt(
                            r -> LEVEL_WEIGHT.getOrDefault(r.intangibleHeritage(), 9)))
                    .toList();

            String topLevel = refs.isEmpty() ? null : refs.get(0).intangibleHeritage();

            all.add(new PersonDirectoryResource.PersonItem(
                    p.getPersonName(),
                    p.getEthnicGroupName(),
                    p.getRoleType(),
                    ROLE_LABELS.getOrDefault(p.getRoleType(), p.getRoleType()),
                    p.getDomain(),
                    p.getLifespan(),
                    p.getBio(),
                    refs,
                    topLevel
            ));
        }

        // 默认排序：传承人在前（名家属历史人物，单独归类更清晰）→ 世界级在前 → 姓名
        all.sort(Comparator
                .comparingInt((PersonDirectoryResource.PersonItem i) -> ROLE_INHERITOR.equals(i.roleType()) ? 0 : 1)
                .thenComparingInt(i -> LEVEL_WEIGHT.getOrDefault(i.topLevel(), 9))
                .thenComparing(PersonDirectoryResource.PersonItem::name));

        // ---- 5) 统计（基于全量，不受筛选影响，便于前端展示筛选计数）----
        PersonDirectoryResource.Summary summary = new PersonDirectoryResource.Summary(
                (int) all.stream().map(PersonDirectoryResource.PersonItem::name).distinct().count(),
                (int) all.stream().filter(i -> ROLE_INHERITOR.equals(i.roleType())).count(),
                (int) all.stream().filter(i -> ROLE_MASTER.equals(i.roleType())).count(),
                (int) all.stream().map(PersonDirectoryResource.PersonItem::ethnicGroupName)
                        .filter(s -> s != null && !s.isBlank()).distinct().count(),
                (int) arts.stream().filter(a -> JsonUtil.toStringArray(a.getInheritors()).length > 0).count()
        );

        PersonDirectoryResource.Filters filters = new PersonDirectoryResource.Filters(
                countBy(all, PersonDirectoryResource.PersonItem::domain),
                countBy(all, PersonDirectoryResource.PersonItem::ethnicGroupName),
                countBy(all, PersonDirectoryResource.PersonItem::roleType,
                        ROLE_LABELS::get)
        );

        // ---- 6) 筛选 ----
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<PersonDirectoryResource.PersonItem> filtered = all.stream()
                .filter(i -> domain == null || domain.isBlank() || domain.equals(i.domain()))
                .filter(i -> ethnic == null || ethnic.isBlank() || ethnic.equals(i.ethnicGroupName()))
                .filter(i -> roleType == null || roleType.isBlank() || roleType.equals(i.roleType()))
                .filter(i -> {
                    if (kw.isEmpty()) {
                        return true;
                    }
                    if (i.name().toLowerCase().contains(kw)) {
                        return true;
                    }
                    // 也支持按项目名搜索人物，例如搜「唐卡」找到两位画师
                    return i.projects().stream()
                            .anyMatch(pr -> pr.name() != null && pr.name().toLowerCase().contains(kw));
                })
                .toList();

        return new PersonDirectoryResource(summary, filters, filtered, filtered.size());
    }

    /** 业务键：姓名 + 民族（同名不同族必须区分） */
    private String key(String name, String ethnic) {
        return (name == null ? "" : name.trim()) + '\u0000' + (ethnic == null ? "" : ethnic.trim());
    }

    /** 按某个字段统计出现次数，生成筛选项（按数量降序） */
    private List<PersonDirectoryResource.Option> countBy(
            List<PersonDirectoryResource.PersonItem> items,
            java.util.function.Function<PersonDirectoryResource.PersonItem, String> keyFn) {
        return countBy(items, keyFn, s -> s);
    }

    private List<PersonDirectoryResource.Option> countBy(
            List<PersonDirectoryResource.PersonItem> items,
            java.util.function.Function<PersonDirectoryResource.PersonItem, String> keyFn,
            java.util.function.Function<String, String> labelFn) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PersonDirectoryResource.PersonItem i : items) {
            String k = keyFn.apply(i);
            if (k == null || k.isBlank()) {
                continue;
            }
            counts.merge(k, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new PersonDirectoryResource.Option(
                        e.getKey(),
                        labelFn.apply(e.getKey()) == null ? e.getKey() : labelFn.apply(e.getKey()),
                        e.getValue()))
                .toList();
    }
}
