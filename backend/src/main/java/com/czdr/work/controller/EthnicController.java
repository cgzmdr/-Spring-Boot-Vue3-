package com.czdr.work.controller;

import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicBriefResource;
import com.czdr.work.model.resource.EthnicInfoDetailedResource;
import com.czdr.work.model.resource.EthnicMapPointResource;
import com.czdr.work.model.resource.EthnicPopulationStatsResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.czdr.work.model.resource.LanguageAtlasResource;
import com.czdr.work.service.EthnicService;
import com.czdr.work.service.LanguageAtlasService;
import com.czdr.work.service.history.EthnicHistoryParser;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * 民族内容接口（C 端公开）
 *
 * @author cz
 */
@Tag(name = "民族 Ethnic", description = "C 端公开内容：民族列表 / 详情 / 56 民族全家福")
@RestController
@RequiredArgsConstructor
@RequestMapping("ethnic-groups")
public class EthnicController {
    private final EthnicService ethnicService;
    private final LanguageAtlasService languageAtlasService;
    private final EthnicHistoryParser ethnicHistoryParser;

    /**
     * 民族列表（分页 + 筛选）
     * <p>支持地域 region、语系 languageFamily、人口范围 population（populationMin/populationMax）、
     * 关键词 keyword（名称/拼音模糊）筛选，sort 支持 population / pinyin / default。</p>
     */
    @Operation(summary = "民族列表", description = "分页查询民族列表，支持地域、语系、人口范围、关键词筛选与排序")
    @GetMapping
    Result<EasyPageResult<EthnicQueryInfoResource>> find(
            EthnicQueryInfoRequest request,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ){
        return Result.success(ethnicService.find(request, pageable));
    };

    /**
     * 56 民族全家福（简略列表）
     * <p>返回全部已发布民族的 id、name、themeColor、coverImage 极简字段，不分页。</p>
     */
    @Operation(summary = "56 民族全家福", description = "返回 56 个民族简略信息（id/name/themeColor/coverImage），供全家福互动墙使用")
    @GetMapping("all")
    Result<List<EthnicBriefResource>> all() {
        List<EthnicBriefResource> list = ethnicService.findAll().stream()
                .map(EthnicConvert::toBriefModel)
                .toList();
        return Result.success(list);
    }

    /**
     * 民族分布地图点位
     * <p>返回全部已发布民族的聚居地（含经纬度），供 C 端地图视图渲染散点 / 气泡。
     * 可选 ethnicGroupId 只看某个民族。数据量约 112 条，不分页。</p>
     */
    @Operation(summary = "民族分布地图", description = "返回已发布民族的聚居地点位（民族/省/市/经纬度/主题色），供地图视图使用")
    @GetMapping("map")
    Result<List<EthnicMapPointResource>> map(
            @RequestParam(value = "ethnicGroupId", required = false)
            @Parameter(description = "可选：只看某个民族的聚居地") String ethnicGroupId) {
        return Result.success(ethnicService.findMapPoints(ethnicGroupId));
    }

    /**
     * 民族人口统计（七普口径）
     * <p>按人口总量、语系、聚居地域与规模分档聚合，供首页轻量可视化。</p>
     */
    @Operation(summary = "民族人口统计", description = "基于 2020 年七普人口数据聚合：Top N、语系分布、地域分布、规模分档")
    @GetMapping("population-stats")
    Result<EthnicPopulationStatsResource> populationStats(
            @RequestParam(value = "topN", defaultValue = "10")
            @Parameter(description = "人口榜单条数（默认 10，最大 56）") int topN) {
        return Result.success(ethnicService.findPopulationStats(topN));
    }

    /**
     * 民族语言文化专栏（B-3）
     * <p>基于各民族的 language_family / languages / scripts 字段聚合，
     * 供「民族语文」专栏页展示语系分布与文字使用情况。</p>
     */
    @Operation(summary = "民族语文专栏", description = "语系分布、文字一览与语言统计，用于民族语言文字专栏页")
    @GetMapping("language-atlas")
    Result<LanguageAtlasResource> languageAtlas() {
        return Result.success(languageAtlasService.atlas());
    }

    /**
     * 民族详情
     * <p>返回完整民族信息，含 customs / festivals / arts / foods / locations 各维度。
     * 另含 {@code history}：由【历史沿革】小节解析出的时间轴 / 时代分期 / 段落索引（方向 C-2），
     * 只收录原文写出明确年份的段落，不做年份推断。</p>
     */
    @Operation(summary = "民族详情", description = "按 ID 查询民族完整信息（含习俗、节日、艺术、美食、聚居地、历史沿革等维度）")
    @GetMapping("{id}")
    Result<EthnicInfoDetailedResource> find(@PathVariable @Parameter(description = "民族 ID") String id) {
        EthnicGroup ethnicGroup = ethnicService.find(id);
        return Result.success(EthnicConvert.toInfoDetailedModel(
                ethnicGroup,
                ethnicHistoryParser.parse(ethnicGroup.getDescription())));
    }
}
