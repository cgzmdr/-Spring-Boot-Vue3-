package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.PersonDirectoryResource;
import com.czdr.work.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人物专栏接口（C 端公开）：民族人物 / 非遗传承人
 *
 * @author cz
 */
@Tag(name = "人物专栏 Persons", description = "C 端公开内容：非遗代表性传承人与历史文化名家名录")
@RestController
@RequiredArgsConstructor
@RequestMapping("persons")
public class PersonsController {

    private final PersonService personService;

    /**
     * 人物名录
     * <p>支持关键词（姓名或项目名）、领域、民族、角色类型组合筛选。</p>
     */
    @Operation(summary = "人物名录", description = "民族人物 / 非遗传承人名录，支持关键词、领域、民族、角色类型筛选")
    @GetMapping
    Result<PersonDirectoryResource> directory(
            @RequestParam(value = "keyword", required = false)
            @Parameter(description = "关键词：姓名或所属非遗项目名") String keyword,
            @RequestParam(value = "domain", required = false)
            @Parameter(description = "领域：音乐 / 舞蹈 / 戏剧 / 服饰 / 技艺 / 建筑") String domain,
            @RequestParam(value = "ethnic", required = false)
            @Parameter(description = "民族名称") String ethnic,
            @RequestParam(value = "roleType", required = false)
            @Parameter(description = "角色类型：inheritor 代表性传承人 / master 历史文化名家") String roleType
    ) {
        return Result.success(personService.directory(keyword, domain, ethnic, roleType));
    }
}
