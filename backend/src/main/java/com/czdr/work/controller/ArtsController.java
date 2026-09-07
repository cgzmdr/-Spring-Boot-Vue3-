package com.czdr.work.controller;

import com.czdr.work.comment.convert.ArtConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.service.ArtService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * @author cz
 */
@Tag(name = "艺术 Arts", description = "C 端公开内容：艺术列表 / 详情")
@RestController
@RequiredArgsConstructor
@RequestMapping("arts")
public class ArtsController {
    private final ArtService artService;

    @Operation(summary = "艺术列表", description = "分页查询艺术列表，支持分类、所属民族、非遗等级筛选")
    @GetMapping
    Result<EasyPageResult<ArtQueryInfoResource>> find(
            ArtQueryInfoRequest request,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "orderNum",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return Result.success(artService.find(request, pageable));
    }

    @Operation(summary = "艺术详情", description = "按 ID 查询艺术完整信息")
    @GetMapping("{id}")
    Result<ArtQueryInfoResource> find(@PathVariable @Parameter(description = "艺术 ID") String id) {
        return Result.success(ArtConvert.toInfoModel(artService.find(id)));
    }
}
