package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.InterestTag;
import com.czdr.work.service.InterestService;
import com.czdr.work.service.SearchIndexService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 检索索引与兴趣标签维护接口（后台，方向 D）。
 *
 * <p>权限沿用既有 {@code translate:glossary}（内容运营域）与 {@code content:manage}，
 * 不新增权限点，避免后台角色配置复杂度上升。</p>
 *
 * @author cz
 */
@Tag(name = "检索与推荐 Admin", description = "后台：检索索引重建、兴趣标签维护")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminSearchController {

    private final SearchIndexService searchIndexService;
    private final InterestService interestService;
    private final EasyEntityQuery entityQuery;

    // ---------------------------------------------------------------- 检索索引

    @Operation(summary = "检索索引统计",
            description = "返回 search_document 各内容类型的文档数，用于确认索引是否覆盖全部内容")
    @SaCheckPermission("translate:glossary")
    @GetMapping("search-index/stats")
    Result<Map<String, Object>> indexStats() {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        long total = 0;
        for (String t : SearchIndexService.DOC_TYPES) {
            long n = entityQuery.queryable(com.czdr.work.model.entity.SearchDocument.class)
                    .where(d -> d.docType().eq(t))
                    .count();
            rows.add(Map.of("docType", t, "count", n));
            total += n;
        }
        return Result.success(Map.of("total", total, "byType", rows,
                "supportedTypes", SearchIndexService.DOC_TYPES));
    }

    @Operation(summary = "重建检索索引",
            description = "重建 search_document。type 留空或 all 为全量重建；内容批量维护后调用即可让新内容立即可搜。")
    @SaCheckPermission("translate:glossary")
    @PostMapping("search-index/rebuild")
    Result<SearchIndexService.RebuildResult> rebuild(
            @RequestParam(value = "type", required = false)
            @Parameter(description = "仅重建指定类型，留空为全量") String type) {
        return Result.success(searchIndexService.rebuild(type));
    }

    // ---------------------------------------------------------------- 兴趣标签

    @Operation(summary = "兴趣标签列表", description = "分页查询兴趣标签，可按维度与关键词筛选")
    @SaCheckPermission("translate:glossary")
    @GetMapping("interest-tags")
    Result<EasyPageResult<InterestTag>> interestTags(
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        EasyPageResult<InterestTag> page = entityQuery.queryable(InterestTag.class)
                .where(t -> {
                    if (dimension != null && !dimension.isBlank()) {
                        t.dimension().eq(dimension);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        t.or(() -> {
                            t.name().like(keyword);
                            t.nameEn().like(keyword);
                        });
                    }
                })
                .orderBy(t -> {
                    t.dimension().asc();
                    t.orderNum().asc();
                })
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return Result.success(page);
    }

    @Operation(summary = "新增 / 更新兴趣标签",
            description = "按 (dimension, name) 唯一；已存在则更新说明/英文名/配色/启停与排序")
    @SaCheckPermission("translate:glossary")
    @PostMapping("interest-tags")
    Result<String> saveInterestTag(@RequestBody InterestTag body) {
        if (body == null || body.getDimension() == null || body.getName() == null
                || body.getName().isBlank()) {
            throw new com.czdr.work.comment.exception.BusinessException(
                    com.czdr.work.comment.exception.ErrorCode.PARAM_ERROR, "dimension 与 name 不能为空");
        }
        InterestTag exist = entityQuery.queryable(InterestTag.class)
                .where(t -> {
                    t.dimension().eq(body.getDimension());
                    t.name().eq(body.getName());
                })
                .firstOrNull();
        if (exist == null) {
            InterestTag e = new InterestTag();
            e.setId(UUID.randomUUID());
            e.setDimension(body.getDimension());
            e.setName(body.getName());
            e.setNameEn(body.getNameEn());
            e.setDescription(body.getDescription());
            e.setColor(body.getColor());
            e.setEnabled(body.getEnabled() == null || body.getEnabled());
            e.setOrderNum(body.getOrderNum() == null ? 0 : body.getOrderNum());
            entityQuery.insertable(e).executeRows();
            return Result.success(e.getId().toString());
        }
        exist.setNameEn(body.getNameEn());
        exist.setDescription(body.getDescription());
        exist.setColor(body.getColor());
        if (body.getEnabled() != null) {
            exist.setEnabled(body.getEnabled());
        }
        if (body.getOrderNum() != null) {
            exist.setOrderNum(body.getOrderNum());
        }
        entityQuery.updatable(exist).executeRows();
        return Result.success(exist.getId().toString());
    }

    @Operation(summary = "删除兴趣标签",
            description = "删除标签及其用户关联（user_interest 由外键级联删除）")
    @SaCheckPermission("translate:glossary")
    @DeleteMapping("interest-tags/{id}")
    Result<Void> deleteInterestTag(@PathVariable String id) {
        entityQuery.deletable(InterestTag.class)
                .where(t -> t.id().eq(UUID.fromString(id)))
                .allowDeleteStatement(true)
                .executeRows();
        return Result.success(null);
    }

    @Operation(summary = "兴趣标签维度统计", description = "各维度标签数，用于后台概览")
    @SaCheckPermission("translate:glossary")
    @GetMapping("interest-tags/stats")
    Result<Map<String, Object>> interestStats() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (String d : List.of("ethnic", "region", "type", "topic")) {
            long n = entityQuery.queryable(InterestTag.class)
                    .where(t -> t.dimension().eq(d))
                    .count();
            out.put(d, n);
        }
        out.put("total", entityQuery.queryable(InterestTag.class).count());
        return Result.success(out);
    }
}
