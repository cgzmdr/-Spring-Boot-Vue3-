package com.czdr.work.controller;

import com.czdr.work.comment.convert.TopicConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.TopicQueryInfoRequest;
import com.czdr.work.model.resource.TopicQueryInfoResource;
import com.czdr.work.service.TopicService;
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
 * 专题内容接口（C 端公开）
 *
 * @author cz
 */
@Tag(name = "专题 Topics", description = "C 端公开内容：专题列表 / 详情")
@RestController
@RequiredArgsConstructor
@RequestMapping("topics")
public class TopicsController {
    private final TopicService topicService;

    /**
     * 专题列表（分页）
     */
    @Operation(summary = "专题列表", description = "分页查询专题列表，按排序号 orderNum 升序排列")
    @GetMapping
    Result<EasyPageResult<TopicQueryInfoResource>> find(
            TopicQueryInfoRequest request,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "orderNum",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return Result.success(topicService.find(request, pageable));
    }

    /**
     * 专题详情
     */
    @Operation(summary = "专题详情", description = "按 ID 查询专题完整信息")
    @GetMapping("{id}")
    Result<TopicQueryInfoResource> find(@PathVariable @Parameter(description = "专题 ID") String id) {
        return Result.success(TopicConvert.toInfoModel(topicService.find(id)));
    }
}
