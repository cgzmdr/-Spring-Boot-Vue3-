package com.czdr.work.model.more;

import java.util.UUID;

/**
 * 民族「轻量投影」结果：人口统计 / 指标聚合只需要这几个字段。
 *
 * <p><b>为什么需要它</b>：{@code ethnic_group} 表只有 56 行，但带有
 * {@code description}（民族简介正文）与 {@code description_en} 两个长文本列，
 * 实测正文合计约 <b>1.1 MB</b>（整行合计约 1792 KB）。
 * 而人口统计、关联指标聚合等场景只用到
 * 名称 / 人口 / 语系 / 地域 这些短字段（合计约 6 KB）。</p>
 *
 * <p>取完整实体等于每次请求都白白传输并反序列化上百 KB 正文；
 * 用本记录承载「只取所需列」的结果，可将该次查询的数据量降低约 300 倍。</p>
 *
 * <p><b>实现注记</b>：当前由 {@code EthnicServiceImpl#findLightweight()} 以
 * 「每列一条单列 select 查询 + 相同排序」的方式组装（本项目既有 EasyQuery 用法
 * 以单列 select 为主，行为最可预期）。若将来 EasyQuery 的 DTO 投影在项目中铺开，
 * 可改为单条投影查询，本记录即为天然载体。</p>
 *
 * @param id             民族 ID
 * @param name           民族名
 * @param population     人口（七普口径）
 * @param languageFamily 语系
 * @param region         主要聚居地（JSON 数组字符串，调用方自行解析）
 */
public record EthnicLightRow(
        UUID id,
        String name,
        long population,
        String languageFamily,
        String region
) {
}