package com.czdr.work.controller;

import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import com.czdr.work.model.resource.EthnicBriefResource;
import com.czdr.work.model.resource.EthnicInfoDetailedResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.czdr.work.service.EthnicService;
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
     * 民族详情
     * <p>返回完整民族信息，含 customs / festivals / arts / foods / locations 各维度。</p>
     */
    @Operation(summary = "民族详情", description = "按 ID 查询民族完整信息（含习俗、节日、艺术、美食、聚居地等维度）")
    @GetMapping("{id}")
    Result<EthnicInfoDetailedResource> find(@PathVariable @Parameter(description = "民族 ID") String id) {
        EthnicGroup ethnicGroup = ethnicService.find(id);
        return Result.success(EthnicConvert.toInfoDetailedModel(ethnicGroup));
    }
}
