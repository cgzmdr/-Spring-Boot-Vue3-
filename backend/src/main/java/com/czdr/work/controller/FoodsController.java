package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.Food;
import com.czdr.work.model.resource.FoodDetailResource;
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
 * 民族美食内容接口（C 端公开）
 */
@Tag(name = "民族美食 Food", description = "C 端公开内容：美食详情")
@RestController
@RequiredArgsConstructor
@RequestMapping("foods")
public class FoodsController {

    private final EthnicService ethnicService;

    @Operation(summary = "美食详情", description = "按 ID 查询民族美食完整内容（含发展沿革）")
    @GetMapping("{id}")
    Result<FoodDetailResource> find(@PathVariable @Parameter(description = "美食 ID") String id) {
        Food food = ethnicService.findFood(id);
        FoodDetailResource resource = new FoodDetailResource(
                food.getId().toString(),
                food.getEthnicGroupId() != null ? food.getEthnicGroupId().toString() : null,
                food.getEthnicGroup() != null ? food.getEthnicGroup().getName() : null,
                food.getName(),
                food.getNameEn(),
                food.getDescription(),
                food.getDescriptionEn(),
                food.getDescriptionEnSource(),
                food.getOrigin(),
                food.getImage(),
                food.getOrderNum()
        );
        return Result.success(resource);
    }
}
