package com.czdr.work.service;

import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author cz
 */
public interface EthnicService {
    EasyPageResult<EthnicQueryInfoResource> find(EthnicQueryInfoRequest request, Pageable pageable);

    EthnicGroup find(String id);

    EthnicCustom findCustom(String id);

    List<EthnicGroup> findAll();
}
