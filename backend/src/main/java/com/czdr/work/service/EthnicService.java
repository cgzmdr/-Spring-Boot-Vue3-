package com.czdr.work.service;

import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicMapPointResource;
import com.czdr.work.model.resource.EthnicPopulationStatsResource;
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

    Food findFood(String id);

    List<EthnicGroup> findAll();

    /**
     * 民族分布地图点位：全部已发布民族的聚居地（经纬度齐全者）。
     *
     * @param ethnicGroupId 可选，限定某个民族
     */
    List<EthnicMapPointResource> findMapPoints(String ethnicGroupId);

    /**
     * 民族人口统计（七普口径）：Top N、语系、地域、分档分布。
     *
     * @param topN 人口榜单取前 N 名
     */
    EthnicPopulationStatsResource findPopulationStats(int topN);
}
