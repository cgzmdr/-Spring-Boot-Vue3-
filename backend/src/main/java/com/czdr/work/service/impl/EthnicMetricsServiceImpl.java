package com.czdr.work.service.impl;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.EthnicLocation;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.more.BasicMetrics;
import com.czdr.work.service.EthnicMetricsService;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 民族列表关联指标聚合实现。
 *
 * <h3>为什么是「一次性加载 + 内存聚合」</h3>
 * 关联表都很小（艺术 165 / 节日 192 / 美食 168 / 风俗 222 / 聚居地 112 /
 * 人物 164 / 自治地方 155 行），而民族固定 56 个。按「每张表一条查询、只取关联列」
 * 共约 7 条查询即可覆盖全部民族，再在内存归并计数；
 * 相比之下「每个民族各查 7 次」最多要 392 条查询，且会随分页重复执行。
 *
 * <h3>缓存</h3>
 * 这些计数只随内容增删变化，属于「变化少、读极多」的数据，
 * 因此结果缓存 60 秒（与 {@code ReadCache} 同思路，但这里用更轻的进程内 Map，
 * 避免为一个派生视图引入额外依赖）。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthnicMetricsServiceImpl implements EthnicMetricsService {

    /** 缓存有效期（毫秒）；指标变化不频繁，60 秒足够且能显著降低列表页查询量 */
    private static final long CACHE_TTL_MS = 60_000L;

    private final EasyEntityQuery entityQuery;

    /** 进程内缓存：民族 ID -> 指标 */
    private volatile Map<UUID, BasicMetrics> cache;
    private volatile long cacheAt;

    @Override
    public Map<UUID, BasicMetrics> loadAll() {
        long now = System.currentTimeMillis();
        Map<UUID, BasicMetrics> snapshot = cache;
        if (snapshot != null && now - cacheAt < CACHE_TTL_MS) {
            return snapshot;
        }
        synchronized (this) {
            if (cache != null && System.currentTimeMillis() - cacheAt < CACHE_TTL_MS) {
                return cache;
            }
            Map<UUID, BasicMetrics> fresh = compute();
            cache = fresh;
            cacheAt = System.currentTimeMillis();
            return fresh;
        }
    }

    /** 主动失效（内容增删改后由管理端调用，让列表页立刻反映新计数） */
    public void invalidate() {
        cache = null;
    }

    private Map<UUID, BasicMetrics> compute() {
        Map<UUID, BasicMetrics> result = new HashMap<>();

        // ---------- 1. 按 ethnic_group_id 直接计数的五类内容 ----------
        // 只 SELECT 关联列（ethnic_group_id），不把整行（含 description 等长文本）拉回来。
        // 这些表里有 150~230 行、每行带数百字到数千字的正文，
        // 取全实体会把大量无用 TEXT 传过网络并反序列化，
        // 是冷启动首次请求偏慢的主因（实测本地冷启动约 600ms、11 条 SQL）。
        Map<UUID, Integer> art = countNonEmpty(
                entityQuery.queryable(Art.class).select(a -> a.ethnicGroupId()).toList());
        Map<UUID, Integer> festival = countNonEmpty(
                entityQuery.queryable(Festival.class).select(f -> f.ethnicGroupId()).toList());
        Map<UUID, Integer> food = countNonEmpty(
                entityQuery.queryable(Food.class).select(f -> f.ethnicGroupId()).toList());
        Map<UUID, Integer> custom = countNonEmpty(
                entityQuery.queryable(EthnicCustom.class).select(c -> c.ethnicGroupId()).toList());
        Map<UUID, Integer> location = countNonEmpty(
                entityQuery.queryable(EthnicLocation.class).select(l -> l.ethnicGroupId()).toList());

        // ---------- 2. 人物：按「民族名」匹配（person_profile.ethnic_group_name 是名称而非外键） ----------
        // 同样只取名称列：person_profile 带 bio（人物小传，长文本）。
        Map<String, Integer> personByName = new HashMap<>();
        try {
            for (String raw : entityQuery.queryable(PersonProfile.class)
                    .select(p -> p.ethnicGroupName())
                    .toList()) {
                String n = normalize(raw);
                if (n != null) {
                    personByName.merge(n, 1, Integer::sum);
                }
            }
        } catch (Exception e) {
            log.warn("统计民族关联人物数失败（按 0 处理）: {}", e.getMessage());
        }

        // ---------- 3. 自治地方：ethnic_groups 是 JSON 数组，一个地方可能含多个民族 ----------
        Map<String, Integer> areaByName = new HashMap<>();
        try {
            for (String raw : entityQuery.queryable(AutonomousArea.class)
                    .select(a -> a.ethnicGroups())
                    .toList()) {
                for (String g : JsonUtil.toStringArray(raw)) {
                    String n = normalize(g);
                    if (n != null) {
                        areaByName.merge(n, 1, Integer::sum);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("统计民族关联自治地方数失败（按 0 处理）: {}", e.getMessage());
        }

        // ---------- 4+5. 人口排名 + 归并 ----------
        // 原先这里查了两次 ethnic_group 且取全实体：该表仅 56 行却有约 1.5MB，
        // 其中 description（民族简介正文）就占约 600KB。
        // 统计关联数只需要「id + name + population」，因此拆成两轮单列投影查询，
        // 避免把 600KB 正文传过网络并反序列化 —— 这是本接口最大的无用开销。
        //
        // 说明：这里不用「一次查询多列投影」，是因为本项目的 EasyQuery 用法
        // （见实体 Proxy 的既有调用点）以单列 select 为主，行为最可预期；
        // 两轮单列查询的总数据量仍远小于一次全实体查询。
        //
        // ⚠️ 三轮查询必须使用**完全相同的排序**，否则按下标对应会错位。
        // 统一按 orderNum 升序（为空排最后），再以 id 兜底保证稳定。
        List<UUID> ids = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc())
                .orderBy(g -> g.id().asc())
                .select(g -> g.id())
                .toList();
        List<String> names = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc())
                .orderBy(g -> g.id().asc())
                .select(g -> g.name())
                .toList();
        List<Long> populations = entityQuery.queryable(EthnicGroup.class)
                .where(g -> g.status().eq("published"))
                .orderBy(g -> g.orderNum().asc())
                .orderBy(g -> g.id().asc())
                .select(g -> g.population())
                .toList();

        // 人口排名：按 population 降序，名次从 1 开始
        List<Long> sortedPop = new ArrayList<>(populations);
        sortedPop.sort(Comparator.reverseOrder());
        // population -> 名次（同名次按先出现的算，人口值唯一时无歧义）
        Map<Long, Integer> rankByPopulation = new HashMap<>();
        for (int i = 0; i < sortedPop.size(); i++) {
            rankByPopulation.putIfAbsent(sortedPop.get(i), i + 1);
        }

        int n = Math.min(ids.size(), Math.min(names.size(), populations.size()));
        for (int i = 0; i < n; i++) {
            UUID id = ids.get(i);
            String name = normalize(names.get(i));
            result.put(id, new BasicMetrics(
                    art.getOrDefault(id, 0),
                    festival.getOrDefault(id, 0),
                    food.getOrDefault(id, 0),
                    custom.getOrDefault(id, 0),
                    location.getOrDefault(id, 0),
                    name == null ? 0 : personByName.getOrDefault(name, 0),
                    name == null ? 0 : areaByName.getOrDefault(name, 0),
                    rankByPopulation.getOrDefault(populations.get(i), 0)
            ));
        }
        return result;
    }

    /** 统计每个民族 ID 出现的次数（忽略 null，即未挂接民族的条目不计入任何民族） */
    private Map<UUID, Integer> countNonEmpty(List<UUID> ids) {
        Map<UUID, Integer> map = new HashMap<>();
        for (UUID id : ids) {
            if (id != null) {
                map.merge(id, 1, Integer::sum);
            }
        }
        return map;
    }

    /** 去掉首尾空白；空串按 null 处理，避免把「」当成一个民族名聚合 */
    private String normalize(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
