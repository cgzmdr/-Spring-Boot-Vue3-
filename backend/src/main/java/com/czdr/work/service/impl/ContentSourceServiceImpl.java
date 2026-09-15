package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ContentSource;
import com.czdr.work.model.entity.ContentSourceLink;
import com.czdr.work.model.resource.ContentSourceResource;
import com.czdr.work.service.ContentSourceService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 内容来源实现（方向 C-1）
 *
 * <p>来源数量很少（当前 6 个），关联关系最多几千条，全部在内存中组装，
 * 与项目其余统计类接口采用同一策略。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class ContentSourceServiceImpl implements ContentSourceService {

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "official", "官方权威",
            "academic", "学术资料",
            "open", "开放图库",
            "other", "其他"
    );

    private static final Map<String, String> METHOD_LABELS = Map.of(
            "scrape", "程序抓取",
            "ocr", "OCR 识别",
            "manual", "人工整理",
            "api", "接口获取"
    );

    private final EasyEntityQuery entityQuery;

    @Override
    public List<ContentSourceResource> findByContent(String targetType, String targetId) {
        if (targetType == null || targetType.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容类型不能为空");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(targetId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容 ID 格式不正确");
        }

        List<ContentSourceLink> links = entityQuery.queryable(ContentSourceLink.class)
                .where(l -> {
                    l.targetType().eq(targetType);
                    l.targetId().eq(uuid);
                })
                .toList();
        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> noteBySource = new LinkedHashMap<>();
        for (ContentSourceLink l : links) {
            noteBySource.putIfAbsent(l.getSourceId(), l.getNote());
        }

        List<ContentSource> sources = entityQuery.queryable(ContentSource.class)
                .where(s -> s.id().in(new ArrayList<>(noteBySource.keySet())))
                .toList();

        return sources.stream()
                .sorted(Comparator
                        .comparingInt((ContentSource s) -> s.getOrderNum() == null ? 999 : s.getOrderNum())
                        .thenComparing(ContentSource::getName))
                .map(s -> toResource(s, noteBySource.get(s.getId())))
                .toList();
    }

    @Override
    public List<ContentSourceResource> findAll() {
        return entityQuery.queryable(ContentSource.class)
                .toList()
                .stream()
                .sorted(Comparator
                        .comparingInt((ContentSource s) -> s.getOrderNum() == null ? 999 : s.getOrderNum())
                        .thenComparing(ContentSource::getName))
                .map(s -> toResource(s, null))
                .toList();
    }

    private ContentSourceResource toResource(ContentSource s, String note) {
        return new ContentSourceResource(
                s.getId().toString(),
                s.getName(),
                s.getPublisher(),
                s.getPublisherShort(),
                s.getDocumentTitle(),
                s.getUrl(),
                s.getSourceType(),
                TYPE_LABELS.getOrDefault(s.getSourceType(), s.getSourceType()),
                s.getCollectMethod(),
                METHOD_LABELS.getOrDefault(s.getCollectMethod(), s.getCollectMethod()),
                s.getRemark(),
                note
        );
    }
}
