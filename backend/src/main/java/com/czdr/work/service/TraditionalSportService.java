package com.czdr.work.service;

import com.czdr.work.model.resource.TraditionalSportResource;

/**
 * 传统体育（B-5）
 *
 * @author cz
 */
public interface TraditionalSportService {

    /**
     * 传统体育名录。
     *
     * @param category 可选，类别过滤（ball / water / strength / accuracy / speed /
     *                 martial / equestrian / gymnastics / swing）
     * @param ethnic   可选，民族名过滤（匹配 ethnic_origins）
     * @param keyword  可选，名称或描述关键词
     */
    TraditionalSportResource directory(String category, String ethnic, String keyword);
}
