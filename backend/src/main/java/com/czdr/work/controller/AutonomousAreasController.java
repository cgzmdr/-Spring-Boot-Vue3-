package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.AutonomousAreaResource;
import com.czdr.work.service.AutonomousAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 民族自治地方接口（C 端公开）
 *
 * @author cz
 */
@Tag(name = "民族自治地方 Autonomous Areas", description = "C 端公开内容：自治区 / 自治州 / 自治县名录")
@RestController
@RequiredArgsConstructor
@RequestMapping("autonomous-areas")
public class AutonomousAreasController {

    private final AutonomousAreaService autonomousAreaService;

    /**
     * 自治地方名录
     * <p>按级别分组，并按自治民族与省级行政区聚合，供「民族自治地方」专栏页使用。</p>
     */
    @Operation(summary = "自治地方名录", description = "5 个自治区 / 30 个自治州 / 120 个自治县·旗，按级别、自治民族、省份聚合")
    @GetMapping
    Result<AutonomousAreaResource> directory(
            @RequestParam(value = "level", required = false)
            @Parameter(description = "级别：autonomous_region / autonomous_prefecture / autonomous_county") String level,
            @RequestParam(value = "keyword", required = false)
            @Parameter(description = "名称关键词") String keyword,
            @RequestParam(value = "ethnic", required = false)
            @Parameter(description = "自治民族名（如「朝鲜族」）") String ethnic
    ) {
        return Result.success(autonomousAreaService.directory(level, keyword, ethnic));
    }
}
