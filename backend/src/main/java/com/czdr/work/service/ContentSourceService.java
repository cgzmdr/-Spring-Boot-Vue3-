package com.czdr.work.service;

import com.czdr.work.model.resource.ContentSourceResource;

import java.util.List;

/**
 * 内容来源（方向 C-1：可溯源）
 *
 * @author cz
 */
public interface ContentSourceService {

    /**
     * 按内容查询来源（详情页「参考资料」区块）。
     *
     * @param targetType ethnic / festival / art / food / topic / person / area / sport
     * @param targetId   内容 ID
     */
    List<ContentSourceResource> findByContent(String targetType, String targetId);

    /**
     * 全部来源（按权威层级与排序号，供「数据来源」汇总页使用）。
     */
    List<ContentSourceResource> findAll();
}
