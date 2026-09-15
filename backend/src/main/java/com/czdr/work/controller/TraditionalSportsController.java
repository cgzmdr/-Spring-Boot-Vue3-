package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.TraditionalSportResource;
import com.czdr.work.service.TraditionalSportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 传统体育接口（C 端公开）
 *
 * @author cz
 */
@Tag(name = "传统体育 Traditional Sports", description = "C 端公开内容：全国少数民族传统体育运动会竞赛项目")
@RestController
@RequiredArgsConstructor
@RequestMapping("traditional-sports")
public class TraditionalSportsController {

    private final TraditionalSportService traditionalSportService;

    /**
     * 传统体育项目名录
     * <p>支持按类别、起源民族、关键词筛选。</p>
     */
    @Operation(summary = "传统体育名录", description = "全国少数民族传统体育运动会竞赛项目，支持类别、民族、关键词筛选")
    @GetMapping
    Result<TraditionalSportResource> directory(
            @RequestParam(value = "category", required = false)
            @Parameter(description = "类别：ball / water / strength / accuracy / speed / martial / equestrian / gymnastics / swing") String category,
            @RequestParam(value = "ethnic", required = false)
            @Parameter(description = "起源民族名（如「满族」）") String ethnic,
            @RequestParam(value = "keyword", required = false)
            @Parameter(description = "名称或描述关键词") String keyword
    ) {
        return Result.success(traditionalSportService.directory(category, ethnic, keyword));
    }
}
