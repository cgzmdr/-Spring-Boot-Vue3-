package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.FormConfig;
import com.czdr.work.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态表单公开接口（C 端）
 * <p>前端通过 id 或 code 获取表单 schema 并动态渲染，无需登录。</p>
 *
 * @author cz
 */
@Tag(name = "动态表单 Form", description = "C 端动态表单：按 id / code 获取启用中的表单配置")
@RestController
@RequiredArgsConstructor
@RequestMapping("forms")
public class FormController {

    private final FormService formService;

    /**
     * 按 ID 获取启用中的表单配置
     */
    @Operation(summary = "按 ID 获取表单", description = "返回启用（active）状态的表单配置（含 schema），供前端按 id 渲染表单")
    @GetMapping("{id}")
    Result<FormConfig> getById(@PathVariable @Parameter(description = "表单配置 ID") String id) {
        return Result.success(formService.getActiveById(id));
    }

    /**
     * 按编码获取启用中的表单配置
     */
    @Operation(summary = "按编码获取表单", description = "按表单编码（code）返回启用状态的表单配置")
    @GetMapping("code/{code}")
    Result<FormConfig> getByCode(@PathVariable @Parameter(description = "表单编码") String code) {
        return Result.success(formService.getActiveByCode(code));
    }
}
