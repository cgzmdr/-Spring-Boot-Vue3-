package com.czdr.work.service;

import com.czdr.work.model.resource.FullTextSearchResource;

/**
 * 全文检索（方向 D）：统一结果流 + 分面 + 高亮。
 *
 * @author cz
 */
public interface FullTextSearchService {

    /**
     * 检索。
     *
     * @param q      检索词（支持中文、中文子串、拼音全拼、拼音首字母、英文；空则按热度返回）
     * @param type   内容类型筛选：ethnic/festival/art/food/custom/person/area/sport，all 表示不限
     * @param ethnic 民族筛选（可选，按 ethnic_name 精确匹配）
     * @param page   页码（0 基）
     * @param size   每页条数
     */
    FullTextSearchResource search(String q, String type, String ethnic, int page, int size);
}
