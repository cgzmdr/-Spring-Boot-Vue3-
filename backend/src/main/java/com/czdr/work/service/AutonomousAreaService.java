package com.czdr.work.service;

import com.czdr.work.model.resource.AutonomousAreaResource;

/**
 * 民族自治地方（B-6）
 *
 * @author cz
 */
public interface AutonomousAreaService {

    /**
     * 自治地方名录：按级别分组 + 按自治民族/省级行政区聚合。
     *
     * @param level    可选，按级别过滤（autonomous_region / autonomous_prefecture / autonomous_county）
     * @param keyword  可选，名称关键词
     * @param ethnic   可选，自治民族名（如「朝鲜族」）—— 返回该民族冠名的自治地方
     */
    AutonomousAreaResource directory(String level, String keyword, String ethnic);
}
