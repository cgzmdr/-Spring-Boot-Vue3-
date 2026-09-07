package com.czdr.work.service;

import com.czdr.work.model.entity.Topic;
import com.czdr.work.model.request.TopicQueryInfoRequest;
import com.czdr.work.model.resource.TopicQueryInfoResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * @author cz
 */
public interface TopicService {
    EasyPageResult<TopicQueryInfoResource> find(TopicQueryInfoRequest request, Pageable pageable);

    Topic find(String id);
}
