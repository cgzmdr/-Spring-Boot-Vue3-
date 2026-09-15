package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.ContentSourceResource;
import com.czdr.work.service.ContentSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容来源接口（C 端公开，方向 C-1：可溯源）
 *
 * @author cz
 */
@Tag(name = "内容来源 Sources", description = "C 端公开内容：数据出处 / 参考资料")
@RestController
@RequiredArgsConstructor
@RequestMapping("sources")
public class ContentSourcesController {

    private final ContentSourceService contentSourceService;

    /**
     * 全部来源
     * <p>供「数据来源」汇总使用（关于页 / 来源页）。</p>
     */
    @Operation(summary = "全部来源", description = "站内数据的所有权威出处，按权威层级与排序号返回")
    @GetMapping
    Result<List<ContentSourceResource>> all() {
        return Result.success(contentSourceService.findAll());
    }

    /**
     * 按内容查询来源
     * <p>详情页「参考资料」区块使用。</p>
     */
    @Operation(summary = "按内容查询来源", description = "查询某条内容的数据出处；targetType 取值 ethnic / festival / art / food / topic / person / area / sport")
    @GetMapping("{targetType}/{targetId}")
    Result<List<ContentSourceResource>> byContent(
            @PathVariable @Parameter(description = "内容类型") String targetType,
            @PathVariable @Parameter(description = "内容 ID") String targetId) {
        return Result.success(contentSourceService.findByContent(targetType, targetId));
    }
}
