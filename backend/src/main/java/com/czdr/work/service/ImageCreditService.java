package com.czdr.work.service;

import com.czdr.work.model.resource.ImageCreditResource;
import com.czdr.work.model.resource.ImageCreditStatsResource;

import java.util.List;

/**
 * 图片版权署名（方向 C-4）
 *
 * @author cz
 */
public interface ImageCreditService {

    /**
     * 按内容查询其所有图片的署名信息（详情页「图片来源」汇总区）。
     *
     * @param targetType ethnic / festival / art / food / topic
     * @param targetId   内容 ID
     */
    List<ImageCreditResource> findByContent(String targetType, String targetId);

    /**
     * 按图片路径查询署名（图片角标按需获取）。
     * <p>支持一次查询多张（逗号分隔），避免图集页逐张请求。</p>
     *
     * @param paths 图片路径，逗号分隔
     */
    List<ImageCreditResource> findByPaths(String paths);

    /** 署名核实进度统计（用于「数据来源」汇总页说明覆盖情况） */
    ImageCreditStatsResource stats();
}
