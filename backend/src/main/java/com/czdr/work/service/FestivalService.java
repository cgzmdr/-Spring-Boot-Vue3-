package com.czdr.work.service;

import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * @author cz
 */
public interface FestivalService {
    EasyPageResult<FestivalQueryInfoResource> find(FestivalQueryInfoRequest request, Pageable pageable);

    Festival find(String id);
}
