package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.ImageCredit;
import com.czdr.work.service.ImageCreditAdminService;
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
 * 后台管理：图片版权署名（方向 C-4）
 *
 * @author cz
 */
@Tag(name = "后台管理 · 图片署名", description = "图片版权核实与署名维护（需 RBAC 权限）")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminImageCreditsController {

    private final ImageCreditAdminService imageCreditAdminService;

    @Operation(summary = "图片署名列表", description = "分页查询，支持关键词与核实状态筛选（需 credit:list 权限）")
    @SaCheckPermission("credit:list")
    @GetMapping("image-credits")
    Result<EasyPageResult<ImageCredit>> list(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "路径 / 说明 / 作者关键词") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "verified / unverified / original") String status,
            @PageableDefault(page = 0, size = 10, sort = "imagePath", direction = Sort.Direction.ASC) Pageable pageable) {
        return Result.success(imageCreditAdminService.list(keyword, status, pageable));
    }

    @Operation(summary = "图片署名详情", description = "按 ID 查询（需 credit:list 权限）")
    @SaCheckPermission("credit:list")
    @GetMapping("image-credits/{id}")
    Result<ImageCredit> get(@PathVariable String id) {
        return Result.success(imageCreditAdminService.get(id));
    }

    @Operation(summary = "核实 / 编辑图片署名",
            description = "需 credit:update 权限；标记为 verified 时必须至少填作者或许可，否则拒绝")
    @SaCheckPermission("credit:update")
    @PutMapping("image-credits/{id}")
    Result<Void> update(@PathVariable String id, @RequestBody ImageCredit body) {
        imageCreditAdminService.update(id, body);
        return Result.success(null);
    }
}
