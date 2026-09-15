package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.ContentSource;
import com.czdr.work.service.ContentSourceAdminService;
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
 * 后台管理：内容来源（方向 C-1）
 *
 * @author cz
 */
@Tag(name = "后台管理 · 内容来源", description = "数据出处 / 参考资料的维护接口（需 RBAC 权限）")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminSourcesController {

    private final ContentSourceAdminService contentSourceAdminService;

    @Operation(summary = "内容来源列表", description = "分页查询，支持关键词与层级筛选（需 source:list 权限）")
    @SaCheckPermission("source:list")
    @GetMapping("sources")
    Result<EasyPageResult<ContentSource>> list(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "名称 / 机构关键词") String keyword,
            @RequestParam(value = "sourceType", required = false) @Parameter(description = "official / academic / open / other") String sourceType,
            @PageableDefault(page = 0, size = 10, sort = "orderNum", direction = Sort.Direction.ASC) Pageable pageable) {
        return Result.success(contentSourceAdminService.list(keyword, sourceType, pageable));
    }

    @Operation(summary = "内容来源详情", description = "按 ID 查询（需 source:list 权限）")
    @SaCheckPermission("source:list")
    @GetMapping("sources/{id}")
    Result<ContentSource> get(@PathVariable String id) {
        return Result.success(contentSourceAdminService.get(id));
    }

    @Operation(summary = "新增内容来源", description = "需 source:create 权限")
    @SaCheckPermission("source:create")
    @PostMapping("sources")
    Result<String> create(@RequestBody ContentSource body) {
        return Result.success(contentSourceAdminService.create(body));
    }

    @Operation(summary = "编辑内容来源", description = "需 source:update 权限")
    @SaCheckPermission("source:update")
    @PutMapping("sources/{id}")
    Result<Void> update(@PathVariable String id, @RequestBody ContentSource body) {
        contentSourceAdminService.update(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除内容来源", description = "需 source:delete 权限；级联删除其内容关联")
    @SaCheckPermission("source:delete")
    @DeleteMapping("sources/{id}")
    Result<Void> delete(@PathVariable String id) {
        contentSourceAdminService.delete(id);
        return Result.success(null);
    }

    @Operation(summary = "来源关联数量", description = "返回每个来源被多少条内容引用（需 source:list 权限）")
    @SaCheckPermission("source:list")
    @GetMapping("sources/{id}/usage")
    Result<Long> usage(@PathVariable String id) {
        return Result.success(contentSourceAdminService.usageCount(id));
    }
}
