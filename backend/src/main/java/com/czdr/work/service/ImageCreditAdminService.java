package com.czdr.work.service;

import com.czdr.work.model.entity.ImageCredit;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * 后台管理：图片版权署名（方向 C-4）
 *
 * @author cz
 */
public interface ImageCreditAdminService {

    /** 列表：支持关键词、核实状态、目录筛选 */
    EasyPageResult<ImageCredit> list(String keyword, String status, Pageable pageable);

    ImageCredit get(String id);

    /**
     * 核实 / 编辑署名。
     * <p>把 {@code creditStatus} 置为 verified 时，要求 author 与 license 至少填一项 ——
     * 否则「已核实」是空壳，页面仍会因 creditLine 为空而无法显示署名。</p>
     */
    void update(String id, ImageCredit body);
}
