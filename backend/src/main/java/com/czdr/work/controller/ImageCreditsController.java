package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.ImageCreditResource;
import com.czdr.work.model.resource.ImageCreditStatsResource;
import com.czdr.work.service.ImageCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图片版权署名接口（C 端公开，方向 C-4）
 *
 * @author cz
 */
@Tag(name = "图片署名 Image Credits", description = "C 端公开内容：图片来源与许可信息")
@RestController
@RequiredArgsConstructor
@RequestMapping("image-credits")
public class ImageCreditsController {

    private final ImageCreditService imageCreditService;

    /**
     * 署名核实进度
     * <p>如实反映「多少张已核实 / 多少张待核」，供「数据来源」汇总页使用。</p>
     */
    @Operation(summary = "署名核实进度", description = "图片总数、已核实数、待核数与核实完成率")
    @GetMapping("stats")
    Result<ImageCreditStatsResource> stats() {
        return Result.success(imageCreditService.stats());
    }

    /**
     * 按图片路径查询署名
     * <p>图片角标按需获取；支持逗号分隔批量查询，避免图集页逐张请求。</p>
     */
    @Operation(summary = "按路径查询署名", description = "paths 为逗号分隔的图片路径，一次可查多张")
    @GetMapping("by-paths")
    Result<List<ImageCreditResource>> byPaths(
            @RequestParam("paths") @Parameter(description = "图片路径，逗号分隔") String paths) {
        return Result.success(imageCreditService.findByPaths(paths));
    }

    /**
     * 按内容查询署名
     * <p>详情页「图片来源」汇总区使用。</p>
     */
    @Operation(summary = "按内容查询署名", description = "targetType 取值 ethnic / festival / art / food / topic")
    @GetMapping("{targetType}/{targetId}")
    Result<List<ImageCreditResource>> byContent(
            @PathVariable @Parameter(description = "内容类型") String targetType,
            @PathVariable @Parameter(description = "内容 ID") String targetId) {
        return Result.success(imageCreditService.findByContent(targetType, targetId));
    }
}
