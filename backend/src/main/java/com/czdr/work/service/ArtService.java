package com.czdr.work.service;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.model.resource.HeritageDirectoryStatsResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * @author cz
 */
public interface ArtService {
    EasyPageResult<ArtQueryInfoResource> find(ArtQueryInfoRequest request, Pageable pageable);

    Art find(String id);

    /**
     * 非遗名录统计概览：级别 / 类别分布、传承人覆盖情况。
     */
    HeritageDirectoryStatsResource heritageStats();
}
