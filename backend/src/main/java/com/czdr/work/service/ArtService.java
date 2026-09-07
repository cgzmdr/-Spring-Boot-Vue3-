package com.czdr.work.service;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * @author cz
 */
public interface ArtService {
    EasyPageResult<ArtQueryInfoResource> find(ArtQueryInfoRequest request, Pageable pageable);

    Art find(String id);
}
