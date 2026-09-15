package com.czdr.work.controller;

import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.CultureTopicResource;
import com.czdr.work.service.CultureTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文化专题接口（C 端公开）：民族服饰 / 民居建筑
 *
 * @author cz
 */
@Tag(name = "文化专题 Culture Topics", description = "C 端公开内容：民族服饰 / 民居建筑专题（合流风俗习惯与非遗项目）")
@RestController
@RequiredArgsConstructor
@RequestMapping("culture-topics")
public class CultureTopicsController {

    private final CultureTopicService cultureTopicService;

    /**
     * 文化专题详情
     * <p>按民族聚合「风俗习惯」与「非遗项目」两个来源的内容。</p>
     */
    @Operation(summary = "文化专题", description = "按民族聚合的专题数据；topic 取值 costume（民族服饰）/ dwelling（民居建筑）")
    @GetMapping("{topic}")
    Result<CultureTopicResource> topic(
            @PathVariable
            @Parameter(description = "专题标识：costume / dwelling") String topic) {
        CultureTopicResource resource = cultureTopicService.topic(topic);
        if (resource == null) {
            return Result.error(ErrorCode.PARAM_ERROR,
                    "不支持的专题：" + topic + "（可选 costume / dwelling）");
        }
        return Result.success(resource);
    }
}
