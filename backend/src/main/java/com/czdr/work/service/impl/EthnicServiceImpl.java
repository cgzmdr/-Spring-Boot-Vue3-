package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.EthnicGroupQueryBind;
import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.proxy.EthnicCustomProxy;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.czdr.work.service.EthnicService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class EthnicServiceImpl implements EthnicService {
    private final EasyEntityQuery entityQuery;
    @Override
    public EasyPageResult<EthnicQueryInfoResource> find(EthnicQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<EthnicGroupProxy>> binds = new EthnicGroupQueryBind(request).customize();
        Map<String, Object> map = request.toMap();
        Sort sort = pageable.getSort();
        EasyPageResult<EthnicGroup> pageResult = entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.status().eq("published");
                    map.forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<EthnicGroupProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(ethnicGroupProxy);
                            }
                        }
                    });
                })
                .orderBy(t -> {
                    // 遍历 Spring 的 Sort 对象
                    for (Sort.Order order : sort) {
                        String property = order.getProperty();
                        boolean isAsc = order.isAscending();
                        switch (property) {
                            case "id":
                                t.id().orderBy(isAsc);
                                break;
                            case "name":
                                t.name().orderBy(isAsc);
                                break;
                            case "population":
                                t.population().orderBy(isAsc);
                                break;
                            case "pinyin":
                                t.pinyin().orderBy(isAsc);
                                break;
                            // 如果有更多字段，继续添加 case...
                            default:
                                // 可选：记录日志或抛出异常，防止前端传入非法字段
                                break;
                        }
                    }
                })
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        List<EthnicQueryInfoResource> data = pageResult.getData().stream()
                .map(EthnicConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public EthnicGroup find(String id) {
        UUID uuid = UUID.fromString(id);
        return entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.id().eq(uuid);
                })
                .include(EthnicGroupProxy::customs)
                .include(EthnicGroupProxy::locations)
                .include(EthnicGroupProxy::foods)
                .include(EthnicGroupProxy::festivals)
                .include(EthnicGroupProxy::arts)
                .firstNotNull();
    }

    @Override
    public EthnicCustom findCustom(String id) {
        UUID uuid = UUID.fromString(id);
        EthnicCustom custom = entityQuery.queryable(EthnicCustom.class)
                .where(customProxy -> {
                    customProxy.id().eq(uuid);
                })
                .include(EthnicCustomProxy::ethnicGroup)
                .firstOrNull();
        if (custom == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return custom;
    }
    @Override
    public List<EthnicGroup> findAll() {
        return entityQuery.queryable(EthnicGroup.class)
                .where(ethnicGroupProxy -> {
                    ethnicGroupProxy.status().eq("published");
                })
                .orderBy(EthnicGroupProxy::orderNum)
                .toList();
    }
}
