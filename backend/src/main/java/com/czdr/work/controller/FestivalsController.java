package com.czdr.work.controller;

import com.czdr.work.comment.convert.FestivalConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import com.czdr.work.model.resource.FestivalCalendarResource;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.czdr.work.service.FestivalService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

/**
 * 节日内容接口（C 端公开）
 *
 * @author cz
 */
@Tag(name = "节日 Festivals", description = "C 端公开内容：节日列表 / 详情")
@RestController
@RequiredArgsConstructor
@RequestMapping("festivals")
public class FestivalsController {
    private final FestivalService festivalService;

    /**
     * 节日列表（分页 + 筛选）
     * <p>支持所属民族 ethnicGroupId、类型 type（traditional/religious/agricultural）、
     * 公历月份 month 筛选。</p>
     */
    @Operation(summary = "节日列表", description = "分页查询节日列表，支持所属民族、类型、公历月份筛选")
    @GetMapping
    Result<EasyPageResult<FestivalQueryInfoResource>> find(
            FestivalQueryInfoRequest request,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "orderNum",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return Result.success(festivalService.find(request, pageable));
    }

    /**
     * 节日日历（按公历月份聚合）
     * <p>159/192 条节日只存了农历表述（如「农历正月初一」），此处统一换算为公历日期；
     * 只精确到月的表述（如「农历八月」）按该月十五估算并标记 approx。</p>
     */
    @Operation(summary = "节日日历", description = "按公历月份聚合全部已发布节日，农历日期自动换算为公历；含今日与未来 30 天节日")
    @GetMapping("calendar")
    Result<FestivalCalendarResource> calendar(
            @RequestParam(value = "year", required = false)
            @Parameter(description = "年份（默认当前年）") Integer year) {
        return Result.success(festivalService.calendar(year));
    }

    /**
     * 节日详情
     */
    @Operation(summary = "节日详情", description = "按 ID 查询节日完整信息")
    @GetMapping("{id}")
    Result<FestivalQueryInfoResource> find(@PathVariable @Parameter(description = "节日 ID") String id) {
        return Result.success(FestivalConvert.toInfoModel(festivalService.find(id)));
    }
}
