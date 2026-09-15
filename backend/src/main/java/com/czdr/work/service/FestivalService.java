package com.czdr.work.service;

import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import com.czdr.work.model.resource.FestivalCalendarResource;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * @author cz
 */
public interface FestivalService {
    EasyPageResult<FestivalQueryInfoResource> find(FestivalQueryInfoRequest request, Pageable pageable);

    Festival find(String id);

    /**
     * 节日日历：把全部已发布节日按公历月份聚合。
     * <p>节日日期可能是公历（33/192）或农历表述（159/192），此处统一换算成目标年的公历日，
     * 并标注日期来源（solar / lunar / approx），无法换算的节日会被跳过。</p>
     *
     * @param year 目标年份；为空时取当前年
     */
    FestivalCalendarResource calendar(Integer year);
}
