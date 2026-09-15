package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.TraditionalSport;
import com.czdr.work.model.request.AutonomousAreaSaveRequest;
import com.czdr.work.model.request.TraditionalSportSaveRequest;
import com.czdr.work.service.BSeriesAdminService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理：B 系列内容（人物档案 / 自治地方 / 传统体育）
 *
 * <p>这三张表是方向 B 新增的内容表，此前只有 C 端展示接口、后台无法维护。
 * 本控制器补齐完整 CRUD，权限点按业务域统一命名为
 * {@code person:*} / {@code area:*} / {@code sport:*}。</p>
 *
 * @author cz
 */
@Tag(name = "后台管理 · B 系列内容", description = "人物档案 / 民族自治地方 / 传统体育的维护接口（需 RBAC 权限）")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminBSeriesController {

    private final BSeriesAdminService bSeriesAdminService;

    /* ===================== 人物档案 person_profile ===================== */

    @Operation(summary = "人物档案列表", description = "分页查询，支持关键词（姓名/民族）与角色类型筛选（需 person:list 权限）")
    @SaCheckPermission("person:list")
    @GetMapping("persons")
    Result<EasyPageResult<PersonProfile>> listPersons(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "姓名或民族关键词") String keyword,
            @RequestParam(value = "roleType", required = false) @Parameter(description = "inheritor / master") String roleType,
            @RequestParam(value = "domain", required = false) @Parameter(description = "领域") String domain,
            @PageableDefault(page = 0, size = 10, sort = "personName", direction = Sort.Direction.ASC) Pageable pageable) {
        return Result.success(bSeriesAdminService.listPersons(keyword, roleType, domain, pageable));
    }

    @Operation(summary = "人物档案详情", description = "按 ID 查询（需 person:list 权限）")
    @SaCheckPermission("person:list")
    @GetMapping("persons/{id}")
    Result<PersonProfile> getPerson(@PathVariable String id) {
        return Result.success(bSeriesAdminService.getPerson(id));
    }

    @Operation(summary = "新增人物档案", description = "需 person:create 权限")
    @SaCheckPermission("person:create")
    @PostMapping("persons")
    Result<String> createPerson(@RequestBody PersonProfile body) {
        return Result.success(bSeriesAdminService.createPerson(body));
    }

    @Operation(summary = "编辑人物档案", description = "需 person:update 权限")
    @SaCheckPermission("person:update")
    @PutMapping("persons/{id}")
    Result<Void> updatePerson(@PathVariable String id, @RequestBody PersonProfile body) {
        bSeriesAdminService.updatePerson(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除人物档案", description = "需 person:delete 权限")
    @SaCheckPermission("person:delete")
    @DeleteMapping("persons/{id}")
    Result<Void> deletePerson(@PathVariable String id) {
        bSeriesAdminService.deletePerson(id);
        return Result.success(null);
    }

    /* ===================== 民族自治地方 autonomous_area ===================== */

    @Operation(summary = "自治地方列表", description = "分页查询，支持关键词与级别筛选（需 area:list 权限）")
    @SaCheckPermission("area:list")
    @GetMapping("areas")
    Result<EasyPageResult<AutonomousArea>> listAreas(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "名称关键词") String keyword,
            @RequestParam(value = "level", required = false) @Parameter(description = "autonomous_region / autonomous_prefecture / autonomous_county") String level,
            @RequestParam(value = "province", required = false) @Parameter(description = "所属省级行政区") String province,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return Result.success(bSeriesAdminService.listAreas(keyword, level, province, pageable));
    }

    @Operation(summary = "自治地方详情", description = "按 ID 查询（需 area:list 权限）")
    @SaCheckPermission("area:list")
    @GetMapping("areas/{id}")
    Result<AutonomousArea> getArea(@PathVariable String id) {
        return Result.success(bSeriesAdminService.getArea(id));
    }

    @Operation(summary = "新增自治地方", description = "需 area:create 权限")
    @SaCheckPermission("area:create")
    @PostMapping("areas")
    Result<String> createArea(@RequestBody AutonomousAreaSaveRequest body) {
        return Result.success(bSeriesAdminService.createArea(body));
    }

    @Operation(summary = "编辑自治地方", description = "需 area:update 权限")
    @SaCheckPermission("area:update")
    @PutMapping("areas/{id}")
    Result<Void> updateArea(@PathVariable String id, @RequestBody AutonomousAreaSaveRequest body) {
        bSeriesAdminService.updateArea(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除自治地方", description = "需 area:delete 权限")
    @SaCheckPermission("area:delete")
    @DeleteMapping("areas/{id}")
    Result<Void> deleteArea(@PathVariable String id) {
        bSeriesAdminService.deleteArea(id);
        return Result.success(null);
    }

    /* ===================== 传统体育 traditional_sport ===================== */

    @Operation(summary = "传统体育列表", description = "分页查询，支持关键词与类别筛选（需 sport:list 权限）")
    @SaCheckPermission("sport:list")
    @GetMapping("sports")
    Result<EasyPageResult<TraditionalSport>> listSports(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "名称关键词") String keyword,
            @RequestParam(value = "category", required = false) @Parameter(description = "类别编码") String category,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return Result.success(bSeriesAdminService.listSports(keyword, category, pageable));
    }

    @Operation(summary = "传统体育详情", description = "按 ID 查询（需 sport:list 权限）")
    @SaCheckPermission("sport:list")
    @GetMapping("sports/{id}")
    Result<TraditionalSport> getSport(@PathVariable String id) {
        return Result.success(bSeriesAdminService.getSport(id));
    }

    @Operation(summary = "新增传统体育项目", description = "需 sport:create 权限")
    @SaCheckPermission("sport:create")
    @PostMapping("sports")
    Result<String> createSport(@RequestBody TraditionalSportSaveRequest body) {
        return Result.success(bSeriesAdminService.createSport(body));
    }

    @Operation(summary = "编辑传统体育项目", description = "需 sport:update 权限")
    @SaCheckPermission("sport:update")
    @PutMapping("sports/{id}")
    Result<Void> updateSport(@PathVariable String id, @RequestBody TraditionalSportSaveRequest body) {
        bSeriesAdminService.updateSport(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除传统体育项目", description = "需 sport:delete 权限")
    @SaCheckPermission("sport:delete")
    @DeleteMapping("sports/{id}")
    Result<Void> deleteSport(@PathVariable String id) {
        bSeriesAdminService.deleteSport(id);
        return Result.success(null);
    }
}
