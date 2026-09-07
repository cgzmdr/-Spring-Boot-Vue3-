package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.TopicQueryBind;
import com.czdr.work.comment.convert.TopicConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Topic;
import com.czdr.work.model.entity.proxy.TopicProxy;
import com.czdr.work.model.request.TopicQueryInfoRequest;
import com.czdr.work.model.resource.TopicQueryInfoResource;
import com.czdr.work.service.TopicService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<TopicQueryInfoResource> find(TopicQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<TopicProxy>> binds = new TopicQueryBind(request).customize();
        EasyPageResult<Topic> pageResult = entityQuery.queryable(Topic.class)
                .where(t -> {
                    t.status().eq("published");
                    request.toMap().forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<TopicProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(t);
                            }
                        }
                    });
                })
                .orderBy(TopicProxy::orderNum)
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<TopicQueryInfoResource> data = pageResult.getData().stream()
                .map(TopicConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public Topic find(String id) {
        UUID uuid = UUID.fromString(id);
        Topic topic = entityQuery.queryable(Topic.class)
                .where(t -> {
                    t.id().eq(uuid);
                    t.status().eq("published");
                })
                .include(TopicProxy::entries)
                .firstOrNull();
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return topic;
    }
}
