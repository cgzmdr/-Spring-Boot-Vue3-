package com.czdr.work.controller;

import com.czdr.work.comment.convert.ArtConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.model.resource.HeritageDirectoryStatsResource;
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
@Tag(name = "艺术 Arts", description = "C 端公开内容：艺术列表 / 详情 / 非遗名录")
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

    /**
     * 非遗名录统计概览
     * <p>级别 / 类别分布与传承人覆盖情况，供名录页顶部概览与筛选条件使用。
     * 注意：需置于 {@code {id}} 之前，避免 "heritage-stats" 被当作 ID 匹配。</p>
     */
    @Operation(summary = "非遗名录统计", description = "非遗项目按级别、类别分布统计，含传承人覆盖情况与聚焦榜")
    @GetMapping("heritage-stats")
    Result<HeritageDirectoryStatsResource> heritageStats() {
        return Result.success(artService.heritageStats());
    }

    @Operation(summary = "艺术详情", description = "按 ID 查询艺术完整信息")
    @GetMapping("{id}")
    Result<ArtQueryInfoResource> find(@PathVariable @Parameter(description = "艺术 ID") String id) {
        return Result.success(ArtConvert.toInfoModel(artService.find(id)));
    }
}
