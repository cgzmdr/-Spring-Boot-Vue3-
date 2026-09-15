package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ContentSource;
import com.czdr.work.model.entity.ContentSourceLink;
import com.czdr.work.service.ContentSourceAdminService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 后台内容来源维护实现（方向 C-1）
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class ContentSourceAdminServiceImpl implements ContentSourceAdminService {

    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<ContentSource> list(String keyword, String sourceType, Pageable pageable) {
        EasyPageResult<ContentSource> page = entityQuery.queryable(ContentSource.class)
                .where(s -> {
                    if (keyword != null && !keyword.isBlank()) {
                        s.or(() -> {
                            s.name().like(keyword);
                            s.publisher().like(keyword);
                            s.publisherShort().like(keyword);
                        });
                    }
                    if (sourceType != null && !sourceType.isBlank()) {
                        s.sourceType().eq(sourceType);
                    }
                })
                .orderBy(s -> s.orderNum().asc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), page.getData());
    }

    @Override
    public ContentSource get(String id) {
        ContentSource s = entityQuery.queryable(ContentSource.class)
                .where(x -> x.id().eq(parseUuid(id)))
                .firstOrNull();
        if (s == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return s;
    }

    @Override
    @Transactional
    public String create(ContentSource body) {
        requireText(body.getName(), "来源名称不能为空");
        assertUnique(body.getName(), null);

        UUID id = UUID.randomUUID();
        body.setId(id);
        if (body.getSourceType() == null || body.getSourceType().isBlank()) {
            body.setSourceType("official");
        }
        if (body.getOrderNum() == null) {
            body.setOrderNum(100);
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
        return id.toString();
    }

    @Override
    @Transactional
    public void update(String id, ContentSource body) {
        ContentSource exist = get(id);
        requireText(body.getName(), "来源名称不能为空");
        assertUnique(body.getName(), parseUuid(id));

        exist.setName(body.getName());
        exist.setPublisher(body.getPublisher());
        exist.setPublisherShort(body.getPublisherShort());
        exist.setDocumentTitle(body.getDocumentTitle());
        exist.setUrl(body.getUrl());
        exist.setSourceType(body.getSourceType());
        exist.setCollectMethod(body.getCollectMethod());
        exist.setRemark(body.getRemark());
        exist.setOrderNum(body.getOrderNum());
        exist.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(exist).executeRows();
    }

    @Override
    @Transactional
    public void delete(String id) {
        UUID uuid = parseUuid(id);
        get(id);
        // 先清理内容关联（表上已有 ON DELETE CASCADE，这里显式删除以便统计返回值与日志）
        entityQuery.deletable(ContentSourceLink.class)
                .allowDeleteStatement(true)
                .where(l -> l.sourceId().eq(uuid))
                .executeRows();
        entityQuery.deletable(ContentSource.class)
                .allowDeleteStatement(true)
                .where(s -> s.id().eq(uuid))
                .executeRows();
    }

    @Override
    public long usageCount(String id) {
        UUID uuid = parseUuid(id);
        get(id);
        return entityQuery.queryable(ContentSourceLink.class)
                .where(l -> l.sourceId().eq(uuid))
                .count();
    }

    private void assertUnique(String name, UUID excludeId) {
        ContentSource dup = entityQuery.queryable(ContentSource.class)
                .where(s -> {
                    s.name().eq(name);
                    if (excludeId != null) {
                        s.id().ne(excludeId);
                    }
                })
                .firstOrNull();
        if (dup != null) {
            throw new BusinessException(ErrorCode.DUPLICATE, "同名的内容来源已存在：" + name);
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 格式不正确");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
}
