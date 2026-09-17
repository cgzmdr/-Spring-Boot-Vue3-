package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.ReadCache;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Topic;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * {@link WorkflowContentGateway} 的默认实现：覆盖民族 / 节日 / 艺术 / 专题四类内容。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowContentGatewayImpl implements WorkflowContentGateway {

    private final EasyEntityQuery entityQuery;
    /** 只读目录缓存：内容状态变化后必须失效，否则已下架内容仍会被 C 端读到 */
    private final ReadCache readCache;

    @Override
    public boolean supports(String entryType) {
        return WorkflowConstants.ENTRY_ETHNIC.equals(entryType)
                || WorkflowConstants.ENTRY_FESTIVAL.equals(entryType)
                || WorkflowConstants.ENTRY_ART.equals(entryType)
                || WorkflowConstants.ENTRY_TOPIC.equals(entryType);
    }

    @Override
    public ContentSnapshot load(String entryType, UUID entryId) {
        return switch (entryType == null ? "" : entryType) {
            case WorkflowConstants.ENTRY_ETHNIC -> {
                EthnicGroup e = entityQuery.queryable(EthnicGroup.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (e == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "民族内容不存在");
                }
                yield new ContentSnapshot(e.getId(), e.getName(), e.getStatus(), e.getContentVersion());
            }
            case WorkflowConstants.ENTRY_FESTIVAL -> {
                Festival f = entityQuery.queryable(Festival.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (f == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "节日内容不存在");
                }
                yield new ContentSnapshot(f.getId(), f.getName(), f.getStatus(), f.getContentVersion());
            }
            case WorkflowConstants.ENTRY_ART -> {
                Art a = entityQuery.queryable(Art.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (a == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "艺术内容不存在");
                }
                yield new ContentSnapshot(a.getId(), a.getName(), a.getStatus(), a.getContentVersion());
            }
            case WorkflowConstants.ENTRY_TOPIC -> {
                Topic t = entityQuery.queryable(Topic.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (t == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "专题内容不存在");
                }
                yield new ContentSnapshot(t.getId(), t.getTitle(), t.getStatus(), t.getContentVersion());
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的内容类型: " + entryType);
        };
    }

    @Override
    public void updateStatus(String entryType, UUID entryId, String status) {
        switch (entryType == null ? "" : entryType) {
            case WorkflowConstants.ENTRY_ETHNIC -> {
                EthnicGroup e = entityQuery.queryable(EthnicGroup.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (e != null) {
                    e.setStatus(status);
                    entityQuery.updatable(e).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_FESTIVAL -> {
                Festival f = entityQuery.queryable(Festival.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (f != null) {
                    f.setStatus(status);
                    entityQuery.updatable(f).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_ART -> {
                Art a = entityQuery.queryable(Art.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (a != null) {
                    a.setStatus(status);
                    entityQuery.updatable(a).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_TOPIC -> {
                Topic t = entityQuery.queryable(Topic.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (t != null) {
                    t.setStatus(status);
                    entityQuery.updatable(t).executeRows();
                }
            }
            default -> log.warn("未知内容类型，忽略状态更新: {}", entryType);
        }
        // 状态变化会直接改变 C 端可见性，必须让目录缓存立即失效
        readCache.invalidateAll();
        reindex(entryType, entryId);
    }

    @Override
    public void bumpContentVersion(String entryType, UUID entryId, int newVersion) {
        switch (entryType == null ? "" : entryType) {
            case WorkflowConstants.ENTRY_ETHNIC -> {
                EthnicGroup e = entityQuery.queryable(EthnicGroup.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (e != null) {
                    e.setContentVersion(newVersion);
                    entityQuery.updatable(e).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_FESTIVAL -> {
                Festival f = entityQuery.queryable(Festival.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (f != null) {
                    f.setContentVersion(newVersion);
                    entityQuery.updatable(f).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_ART -> {
                Art a = entityQuery.queryable(Art.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (a != null) {
                    a.setContentVersion(newVersion);
                    entityQuery.updatable(a).executeRows();
                }
            }
            case WorkflowConstants.ENTRY_TOPIC -> {
                Topic t = entityQuery.queryable(Topic.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (t != null) {
                    t.setContentVersion(newVersion);
                    entityQuery.updatable(t).executeRows();
                }
            }
            default -> log.warn("未知内容类型，忽略版本更新: {}", entryType);
        }
    }
}
