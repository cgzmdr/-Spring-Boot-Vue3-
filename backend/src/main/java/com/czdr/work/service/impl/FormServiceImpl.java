package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.FormConfig;
import com.czdr.work.service.FormService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {

    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<FormConfig> list(String keyword, String status, Pageable pageable) {
        return entityQuery.queryable(FormConfig.class)
                .where(f -> {
                    if (StringUtils.hasText(status)) {
                        f.status().eq(status);
                    }
                    if (StringUtils.hasText(keyword)) {
                        f.or(() -> {
                            f.name().like(keyword);
                            f.code().like(keyword);
                        });
                    }
                })
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public FormConfig getById(String id) {
        FormConfig entity = entityQuery.queryable(FormConfig.class)
                .where(f -> f.id().eq(UUID.fromString(id)))
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    @Override
    public void create(FormConfig body) {
        body.setId(UUID.randomUUID());
        if (!StringUtils.hasText(body.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单编码不能为空");
        }
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("active");
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    public void update(String id, FormConfig body) {
        FormConfig target = getById(id);
        if (StringUtils.hasText(body.getCode())) {
            target.setCode(body.getCode());
        }
        if (body.getName() != null) {
            target.setName(body.getName());
        }
        if (body.getDescription() != null) {
            target.setDescription(body.getDescription());
        }
        if (body.getSchema() != null) {
            target.setSchema(body.getSchema());
        }
        if (StringUtils.hasText(body.getStatus())) {
            target.setStatus(body.getStatus());
        }
        target.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(target).executeRows();
    }

    @Override
    public void delete(String id) {
        entityQuery.deletable(getById(id)).allowDeleteStatement(true).executeRows();
    }

    @Override
    public FormConfig getActiveById(String id) {
        FormConfig entity = entityQuery.queryable(FormConfig.class)
                .where(f -> {
                    f.id().eq(UUID.fromString(id));
                    f.status().eq("active");
                })
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    @Override
    public FormConfig getActiveByCode(String code) {
        FormConfig entity = entityQuery.queryable(FormConfig.class)
                .where(f -> {
                    f.code().eq(code);
                    f.status().eq("active");
                })
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }
}
