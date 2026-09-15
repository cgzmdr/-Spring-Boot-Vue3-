package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.FullTextSearchResource;
import com.czdr.work.model.resource.SearchResultResource;
import com.czdr.work.service.FullTextSearchService;
import com.czdr.work.service.SearchIndexService;
import com.czdr.work.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索接口（公开）
 *
 * <p>两套检索并存：</p>
 * <ul>
 *   <li>{@code GET /search}：旧的分组式检索（民族/节日/艺术三组），保留以兼容既有前端；</li>
 *   <li>{@code GET /search/full}：方向 D 的统一全文检索（8 类内容混排 + 分面 + 高亮 + 拼音）。</li>
 * </ul>
 *
 * @author cz
 */
@Tag(name = "搜索 Search", description = "C 端公开：全文搜索 / 统一检索 / 热门搜索词")
@RestController
@RequiredArgsConstructor
@RequestMapping("search")
public class SearchController {
    private final SearchService searchService;
    private final FullTextSearchService fullTextSearchService;
    private final SearchIndexService searchIndexService;

    /**
     * 全文搜索（分页）
     * <p>关键词 q 必填（支持拼音，如 zangzu），type 限定类型：ethnic / festival / art / all，
     * 结果按类型分组返回。</p>
     */
    @Operation(summary = "全文搜索", description = "按关键词搜索民族、节日、艺术内容（支持拼音），可按类型限定并分页返回")
    @GetMapping
    Result<SearchResultResource> search(
            @RequestParam("q") @Parameter(description = "搜索关键词，支持拼音（如 zangzu）") String q,
            @RequestParam(value = "type", required = false) @Parameter(description = "限定类型：ethnic / festival / art / all，默认 all") String type,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return Result.success(searchService.search(q, type,
                pageable.getPageNumber(), pageable.getPageSize()));
    }

    /**
     * 热门搜索词
     * <p>返回 Redis 排行榜前 N 个热门词。</p>
     */
    @Operation(summary = "热门搜索词", description = "返回热门搜索词排行榜（Redis ZSet 存储）")
    @GetMapping("hot")
    Result<List<String>> hot() {
        return Result.success(searchService.hot());
    }

    // ---------------------------------------------------------------- 方向 D：统一全文检索

    /**
     * 统一全文检索
     * <p>覆盖 8 类内容（民族/节日/艺术/美食/风俗/人物/自治地方/传统体育），
     * 支持中文子串、拼音全拼、拼音首字母、英文，返回分面计数与关键词高亮。</p>
     */
    @Operation(summary = "统一全文检索",
            description = "8 类内容混排并按相关度排序。q 支持中文、中文子串、拼音全拼（mengguzu）、"
                    + "首字母（mgz）、英文；type 过滤类型，ethnic 过滤民族；返回 facets 分面与 titleHtml 高亮。")
    @GetMapping("full")
    Result<FullTextSearchResource> full(
            @RequestParam(value = "q", required = false) @Parameter(description = "检索词") String q,
            @RequestParam(value = "type", required = false, defaultValue = "all")
            @Parameter(description = "内容类型：ethnic/festival/art/food/custom/person/area/sport，all 不限") String type,
            @RequestParam(value = "ethnic", required = false) @Parameter(description = "民族筛选") String ethnic,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.success(fullTextSearchService.search(q, type, ethnic,
                Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }

    /**
     * 重建检索索引
     * <p>把 8 类内容重新聚合进 search_document。内容批量维护后调用即可让新内容立即可搜。</p>
     */
    @Operation(summary = "重建检索索引",
            description = "重建 search_document 索引（type 留空或 all 为全量重建），返回各类型条数与耗时")
    @PostMapping("reindex")
    Result<SearchIndexService.RebuildResult> reindex(
            @RequestParam(value = "type", required = false)
            @Parameter(description = "仅重建指定类型，留空为全量") String type) {
        return Result.success(searchIndexService.rebuild(type));
    }
}
