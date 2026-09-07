package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.FestivalQueryBind;
import com.czdr.work.comment.convert.FestivalConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.proxy.FestivalProxy;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.czdr.work.service.FestivalService;
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
public class FestivalServiceImpl implements FestivalService {
    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<FestivalQueryInfoResource> find(FestivalQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<FestivalProxy>> binds = new FestivalQueryBind(request).customize();
        EasyPageResult<Festival> pageResult = entityQuery.queryable(Festival.class)
                .where(f -> {
                    f.status().eq("published");
                    request.toMap().forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<FestivalProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(f);
                            }
                        }
                    });
                })
                .include(FestivalProxy::ethnicGroup)
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<FestivalQueryInfoResource> data = pageResult.getData().stream()
                .map(FestivalConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public Festival find(String id) {
        UUID uuid = UUID.fromString(id);
        Festival festival = entityQuery.queryable(Festival.class)
                .where(f -> {
                    f.id().eq(uuid);
                    f.status().eq("published");
                })
                .include(FestivalProxy::ethnicGroup)
                .firstOrNull();
        if (festival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return festival;
    }
}
