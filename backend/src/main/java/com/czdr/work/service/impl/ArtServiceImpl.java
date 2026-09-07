package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.ArtQueryBind;
import com.czdr.work.comment.convert.ArtConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.service.ArtService;
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
public class ArtServiceImpl implements ArtService {
    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<ArtQueryInfoResource> find(ArtQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<ArtProxy>> binds = new ArtQueryBind(request).customize();
        EasyPageResult<Art> pageResult = entityQuery.queryable(Art.class)
                .where(art -> {
                    art.status().eq("published");
                    request.toMap().forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<ArtProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(art);
                            }
                        }
                    });
                })
                .include(ArtProxy::ethnicGroup)
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<ArtQueryInfoResource> data = pageResult.getData().stream()
                .map(ArtConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public Art find(String id) {
        UUID uuid = UUID.fromString(id);
        Art art = entityQuery.queryable(Art.class)
                .where(a -> {
                    a.id().eq(uuid);
                    a.status().eq("published");
                })
                .include(ArtProxy::ethnicGroup)
                .firstOrNull();
        if (art == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return art;
    }
}
