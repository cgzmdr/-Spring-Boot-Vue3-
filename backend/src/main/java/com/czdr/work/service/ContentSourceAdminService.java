package com.czdr.work.service;

import com.czdr.work.model.entity.ContentSource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * 后台管理：内容来源（方向 C-1）
 *
 * @author cz
 */
public interface ContentSourceAdminService {

    EasyPageResult<ContentSource> list(String keyword, String sourceType, Pageable pageable);

    ContentSource get(String id);

    String create(ContentSource body);

    void update(String id, ContentSource body);

    /** 删除来源，并级联清理其内容关联 */
    void delete(String id);

    /** 该来源被多少条内容引用 */
    long usageCount(String id);
}
