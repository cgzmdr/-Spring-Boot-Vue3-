package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.EthnicCustom;
import com.czdr.work.model.resource.EthnicCustomDetailResource;
import com.czdr.work.service.EthnicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 民族风俗习惯内容接口（C 端公开）
 */
@Tag(name = "民族风俗习惯 Ethnic Customs", description = "C 端公开内容：风俗习惯详情")
@RestController
@RequiredArgsConstructor
@RequestMapping("ethnic-customs")
public class EthnicCustomsController {

    private final EthnicService ethnicService;

    @Operation(summary = "风俗习惯详情", description = "按 ID 查询民族风俗习惯完整内容")
    @GetMapping("{id}")
    Result<EthnicCustomDetailResource> find(@PathVariable @Parameter(description = "风俗习惯 ID") String id) {
        EthnicCustom custom = ethnicService.findCustom(id);
        EthnicCustomDetailResource resource = new EthnicCustomDetailResource(
                custom.getId().toString(),
                custom.getEthnicGroupId() != null ? custom.getEthnicGroupId().toString() : null,
                custom.getEthnicGroup() != null ? custom.getEthnicGroup().getName() : null,
                custom.getCategory(),
                custom.getTitle(),
                custom.getContent(),
                custom.getImage(),
                custom.getOrderNum()
        );
        return Result.success(resource);
    }
}
