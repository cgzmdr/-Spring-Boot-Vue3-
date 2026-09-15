package com.czdr.work.service;

import com.czdr.work.model.resource.CultureTopicResource;

/**
 * 文化专题聚合（B-1 民族服饰 / B-2 民居建筑）
 *
 * @author cz
 */
public interface CultureTopicService {

    /** 支持的专题标识 */
    String TOPIC_COSTUME = "costume";
    String TOPIC_DWELLING = "dwelling";

    /**
     * 构建专题数据：按民族合流「风俗习惯」与「非遗项目」两个来源。
     *
     * @param topic 专题标识（costume / dwelling）
     * @return 专题聚合结果；未知标识返回 {@code null}
     */
    CultureTopicResource topic(String topic);
}
