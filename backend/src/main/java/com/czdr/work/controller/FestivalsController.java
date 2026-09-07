package com.czdr.work.controller;

import com.czdr.work.comment.convert.FestivalConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
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
     * 节日详情
     */
    @Operation(summary = "节日详情", description = "按 ID 查询节日完整信息")
    @GetMapping("{id}")
    Result<FestivalQueryInfoResource> find(@PathVariable @Parameter(description = "节日 ID") String id) {
        return Result.success(FestivalConvert.toInfoModel(festivalService.find(id)));
    }
}
