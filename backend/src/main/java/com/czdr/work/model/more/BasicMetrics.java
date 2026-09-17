package com.czdr.work.model.more;

/**
 * 民族列表项附带的关联内容计数。
 *
 * <p>用于 C 端「民族」列表页的摘要指标展示：把原先需要点进详情才能看到的信息
 * （关联非遗、节日、美食、风俗、聚居地、人物、自治地方、人口排名）提前到列表卡片上。</p>
 *
 * <p>全部计数来自库中真实数据的聚合，缺失一律为 0 —— 不臆造、不估算。</p>
 *
 * @param artCount            关联艺术/非遗条目数
 * @param festivalCount       关联节日数
 * @param foodCount           关联美食数
 * @param customCount         关联风俗条目数
 * @param locationCount       聚居地数量
 * @param personCount         关联人物数
 * @param autonomousAreaCount 自治地方数量（以该民族为自治民族）
 * @param populationRank      人口排名（1 = 人口最多）
 */
public record BasicMetrics(
        int artCount,
        int festivalCount,
        int foodCount,
        int customCount,
        int locationCount,
        int personCount,
        int autonomousAreaCount,
        int populationRank
) {

    /** 空指标（全部为 0），供不需要指标的调用点复用 */
    public static final BasicMetrics EMPTY = new BasicMetrics(0, 0, 0, 0, 0, 0, 0, 0);
}
