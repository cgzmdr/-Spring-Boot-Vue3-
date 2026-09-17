package com.czdr.work.service;

import com.czdr.work.model.more.BasicMetrics;

import java.util.Map;
import java.util.UUID;

/**
 * 民族列表的关联指标聚合。
 *
 * <p>把「每条内容关联了多少其他内容」算出来供列表页展示。</p>
 *
 * <h3>实现取向：一次性加载 + 内存聚合，而不是 56×(N+1) 次 count</h3>
 * 关联表规模很小（art 165 / festival 192 / food 168 / ethnic_custom 222 /
 * ethnic_location 112 / person_profile 164 / autonomous_area 155），
 * 且民族固定 56 个。因此按「每张表一条查询只取关联列」的方式加载，
 * 再在内存里归并计数：总查询数是常量级（约 7 条），
 * 比按民族逐条 count（最多 56×7=392 条）快得多，也不会随分页漂移。
 *
 * @author cz
 */
public interface EthnicMetricsService {

    /**
     * 一次算出全部民族的指标。
     * <p>调用方拿到的 Map 按民族 ID 索引；缺失的键由调用方按
     * {@link BasicMetrics#EMPTY} 处理。</p>
     */
    Map<UUID, BasicMetrics> loadAll();
}
