package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.SearchResultResource;
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
 * @author cz
 */
@Tag(name = "搜索 Search", description = "C 端公开：全文搜索 / 热门搜索词")
@RestController
@RequiredArgsConstructor
@RequestMapping("search")
public class SearchController {
    private final SearchService searchService;

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
}
