package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.*;
import com.czdr.work.model.request.AdminUserSaveRequest;
import com.czdr.work.model.request.ReviewRejectRequest;
import com.czdr.work.model.resource.ContentStatsResource;
import com.czdr.work.model.resource.StatsOverviewResource;
import com.czdr.work.service.AdminService;
import com.czdr.work.service.FormService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author cz
 */
@Tag(name = "后台管理 Admin", description = "CMS 管理端（需登录 + RBAC 权限）：民族 / 节日 / 艺术 / 专题管理、内容审核、用户与角色管理、统计报表")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminController {
    private final AdminService adminService;
    private final FormService formService;

    @Operation(summary = "新增民族", description = "新增民族内容（需 ethnic:create 权限）")
    @SaCheckPermission("ethnic:create")
    @PostMapping("ethnic-groups")
    Result<Void> createEthnicGroup(@RequestBody EthnicGroup body) {
        adminService.createEthnicGroup(body);
        return Result.success(null);
    }

    @Operation(summary = "编辑民族", description = "按 ID 编辑民族内容（需 ethnic:update 权限）")
    @SaCheckPermission("ethnic:update")
    @PutMapping("ethnic-groups/{id}")
    Result<Void> updateEthnicGroup(@PathVariable @Parameter(description = "民族 ID") String id, @RequestBody EthnicGroup body) {
        adminService.updateEthnicGroup(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除民族", description = "按 ID 删除民族（需 ethnic:delete 权限）")
    @SaCheckPermission("ethnic:delete")
    @DeleteMapping("ethnic-groups/{id}")
    Result<Void> deleteEthnicGroup(@PathVariable @Parameter(description = "民族 ID") String id) {
        adminService.deleteEthnicGroup(id);
        return Result.success(null);
    }

    @Operation(summary = "民族管理列表", description = "后台分页查询民族列表（含草稿/审核中），支持关键词与状态筛选（需 ethnic:list 权限）")
    @SaCheckPermission("ethnic:list")
    @GetMapping("ethnic-groups")
    Result<EasyPageResult<EthnicGroup>> listEthnicGroups(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（名称/拼音模糊匹配）") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "内容状态：draft / pending / published / rejected") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listEthnicGroups(keyword, status, pageable));
    }

    @Operation(summary = "民族后台详情", description = "按 ID 查询民族后台详情（需 ethnic:view 权限）")
    @SaCheckPermission("ethnic:view")
    @GetMapping("ethnic-groups/{id}")
    Result<EthnicGroup> getEthnicGroup(@PathVariable @Parameter(description = "民族 ID") String id) {
        return Result.success(adminService.getEthnicGroup(id));
    }

    @Operation(summary = "新增节日", description = "新增节日内容（需 festival:create 权限）")
    @SaCheckPermission("festival:create")
    @PostMapping("festivals")
    Result<Void> createFestival(@RequestBody Festival body) {
        adminService.createFestival(body);
        return Result.success(null);
    }

    @Operation(summary = "编辑节日", description = "按 ID 编辑节日内容（需 festival:update 权限）")
    @SaCheckPermission("festival:update")
    @PutMapping("festivals/{id}")
    Result<Void> updateFestival(@PathVariable @Parameter(description = "节日 ID") String id, @RequestBody Festival body) {
        adminService.updateFestival(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除节日", description = "按 ID 删除节日（需 festival:delete 权限）")
    @SaCheckPermission("festival:delete")
    @DeleteMapping("festivals/{id}")
    Result<Void> deleteFestival(@PathVariable @Parameter(description = "节日 ID") String id) {
        adminService.deleteFestival(id);
        return Result.success(null);
    }

    @Operation(summary = "节日后台详情", description = "按 ID 查询节日后台详情（需 festival:list 权限）")
    @SaCheckPermission("festival:list")
    @GetMapping("festivals/{id}")
    Result<Festival> getFestival(@PathVariable @Parameter(description = "节日 ID") String id) {
        return Result.success(adminService.getFestival(id));
    }

    @Operation(summary = "节日管理列表", description = "后台分页查询节日列表（含草稿/审核中），支持关键词与状态筛选（需 festival:list 权限）")
    @SaCheckPermission("festival:list")
    @GetMapping("festivals")
    Result<EasyPageResult<Festival>> listFestivals(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（名称模糊匹配）") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "内容状态：draft / pending / published / rejected") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listFestivals(keyword, status, pageable));
    }

    @Operation(summary = "新增艺术", description = "新增艺术内容（需 art:create 权限）")
    @SaCheckPermission("art:create")
    @PostMapping("arts")
    Result<Void> createArt(@RequestBody Art body) {
        adminService.createArt(body);
        return Result.success(null);
    }

    @Operation(summary = "编辑艺术", description = "按 ID 编辑艺术内容（需 art:update 权限）")
    @SaCheckPermission("art:update")
    @PutMapping("arts/{id}")
    Result<Void> updateArt(@PathVariable @Parameter(description = "艺术 ID") String id, @RequestBody Art body) {
        adminService.updateArt(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除艺术", description = "按 ID 删除艺术（需 art:delete 权限）")
    @SaCheckPermission("art:delete")
    @DeleteMapping("arts/{id}")
    Result<Void> deleteArt(@PathVariable @Parameter(description = "艺术 ID") String id) {
        adminService.deleteArt(id);
        return Result.success(null);
    }

    @Operation(summary = "艺术后台详情", description = "按 ID 查询艺术后台详情（需 art:list 权限）")
    @SaCheckPermission("art:list")
    @GetMapping("arts/{id}")
    Result<Art> getArt(@PathVariable @Parameter(description = "艺术 ID") String id) {
        return Result.success(adminService.getArt(id));
    }

    @Operation(summary = "艺术管理列表", description = "后台分页查询艺术列表（含草稿/审核中），支持关键词与状态筛选（需 art:list 权限）")
    @SaCheckPermission("art:list")
    @GetMapping("arts")
    Result<EasyPageResult<Art>> listArts(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（名称模糊匹配）") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "内容状态：draft / pending / published / rejected") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listArts(keyword, status, pageable));
    }

    @Operation(summary = "新增专题", description = "新增专题内容（需 topic:create 权限）")
    @SaCheckPermission("topic:create")
    @PostMapping("topics")
    Result<Void> createTopic(@RequestBody Topic body) {
        adminService.createTopic(body);
        return Result.success(null);
    }

    @Operation(summary = "编辑专题", description = "按 ID 编辑专题内容（需 topic:update 权限）")
    @SaCheckPermission("topic:update")
    @PutMapping("topics/{id}")
    Result<Void> updateTopic(@PathVariable @Parameter(description = "专题 ID") String id, @RequestBody Topic body) {
        adminService.updateTopic(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除专题", description = "按 ID 删除专题（需 topic:delete 权限）")
    @SaCheckPermission("topic:delete")
    @DeleteMapping("topics/{id}")
    Result<Void> deleteTopic(@PathVariable @Parameter(description = "专题 ID") String id) {
        adminService.deleteTopic(id);
        return Result.success(null);
    }

    @Operation(summary = "专题后台详情", description = "按 ID 查询专题后台详情（需 topic:list 权限）")
    @SaCheckPermission("topic:list")
    @GetMapping("topics/{id}")
    Result<Topic> getTopic(@PathVariable @Parameter(description = "专题 ID") String id) {
        return Result.success(adminService.getTopic(id));
    }

    @Operation(summary = "专题管理列表", description = "后台分页查询专题列表（含草稿/审核中），支持关键词与状态筛选（需 topic:list 权限）")
    @SaCheckPermission("topic:list")
    @GetMapping("topics")
    Result<EasyPageResult<Topic>> listTopics(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（名称模糊匹配）") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "内容状态：draft / pending / published / rejected") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listTopics(keyword, status, pageable));
    }

    @Operation(summary = "新增表单配置", description = "新增动态表单配置（需 form:create 权限）")
    @SaCheckPermission("form:create")
    @PostMapping("forms")
    Result<Void> createForm(@RequestBody FormConfig body) {
        formService.create(body);
        return Result.success(null);
    }

    @Operation(summary = "编辑表单配置", description = "按 ID 编辑动态表单配置（需 form:update 权限）")
    @SaCheckPermission("form:update")
    @PutMapping("forms/{id}")
    Result<Void> updateForm(@PathVariable @Parameter(description = "表单配置 ID") String id, @RequestBody FormConfig body) {
        formService.update(id, body);
        return Result.success(null);
    }

    @Operation(summary = "删除表单配置", description = "按 ID 删除动态表单配置（需 form:delete 权限）")
    @SaCheckPermission("form:delete")
    @DeleteMapping("forms/{id}")
    Result<Void> deleteForm(@PathVariable @Parameter(description = "表单配置 ID") String id) {
        formService.delete(id);
        return Result.success(null);
    }

    @Operation(summary = "表单配置详情", description = "按 ID 查询表单配置详情（需 form:list 权限）")
    @SaCheckPermission("form:list")
    @GetMapping("forms/{id}")
    Result<FormConfig> getForm(@PathVariable @Parameter(description = "表单配置 ID") String id) {
        return Result.success(formService.getById(id));
    }

    @Operation(summary = "表单配置列表", description = "后台分页查询表单配置，支持关键词与状态筛选（需 form:list 权限）")
    @SaCheckPermission("form:list")
    @GetMapping("forms")
    Result<com.easy.query.core.api.pagination.EasyPageResult<FormConfig>> listForms(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（名称/编码模糊匹配）") String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "状态：active / disabled") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(formService.list(keyword, status, pageable));
    }

    @Operation(summary = "审核列表", description = "分页查询待审内容，可按审核状态筛选（需 review:list 权限）")
    @SaCheckPermission("review:list")
    @GetMapping("reviews")
    Result<EasyPageResult<ContentReview>> listReviews(
            @RequestParam(value = "status", required = false) @Parameter(description = "审核状态：pending / approved / rejected，为空返回全部") String status,
            @PageableDefault(page = 0, size = 10, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listReviews(status, pageable));
    }

    @SaCheckRole("super_admin")
    @GetMapping("init")
    void init() {
        adminService.initReview();
    }

    @Operation(summary = "审核通过", description = "通过指定内容的审核并发布（需 review:approve 权限）")
    @SaCheckPermission("review:approve")
    @PostMapping("reviews/{id}/approve")
    Result<Void> approveReview(@PathVariable @Parameter(description = "审核记录 ID") String id) {
        adminService.approveReview(id);
        return Result.success(null);
    }

    /**
     * 审核驳回
     */
    @Operation(summary = "审核驳回", description = "驳回指定内容，可附驳回原因（需 review:reject 权限）")
    @SaCheckPermission("review:reject")
    @PostMapping("reviews/{id}/reject")
    Result<Void> rejectReview(@PathVariable @Parameter(description = "审核记录 ID") String id, @RequestBody(required = false) ReviewRejectRequest request) {
        adminService.rejectReview(id, request == null ? null : request.reason());
        return Result.success(null);
    }

    // ==================== 用户与角色 ====================

    /**
     * 用户列表（分页）
     */
    @Operation(summary = "用户列表", description = "后台分页查询用户列表，支持关键词筛选（需 user:list 权限）")
    @SaCheckPermission("user:list")
    @GetMapping("users")
    Result<EasyPageResult<UserAuth>> listUsers(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（昵称/账号模糊匹配）") String keyword,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listUsers(keyword, pageable));
    }

    /**
     * 用户详情
     */
    @Operation(summary = "用户详情", description = "按 ID 查询用户信息（需 user:view 权限）")
    @SaCheckPermission("user:view")
    @GetMapping("users/{id}")
    Result<UserAuth> getUser(@PathVariable @Parameter(description = "用户 ID") String id) {
        return Result.success(adminService.getUser(id));
    }

    /**
     * 新建用户（超级管理员创建测试账号）
     */
    @Operation(summary = "新建用户", description = "创建用户并绑定角色，返回新用户 ID（需 user:create 权限）")
    @SaCheckPermission("user:create")
    @PostMapping("users")
    Result<String> createUser(@RequestBody AdminUserSaveRequest request) {
        return Result.success(adminService.createUser(request));
    }

    /**
     * 编辑用户
     */
    @Operation(summary = "编辑用户", description = "按 ID 修改用户信息（昵称/账号/联系方式/状态/重置密码/角色），未提交的字段保持不变（需 user:update 权限）")
    @SaCheckPermission("user:update")
    @PutMapping("users/{id}")
    Result<Void> updateUser(@PathVariable @Parameter(description = "用户 ID") String id, @RequestBody AdminUserSaveRequest request) {
        adminService.updateUser(id, request);
        return Result.success(null);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "按 ID 删除用户及其角色/收藏/点赞关联数据（需 user:delete 权限）")
    @SaCheckPermission("user:delete")
    @DeleteMapping("users/{id}")
    Result<Void> deleteUser(@PathVariable @Parameter(description = "用户 ID") String id) {
        adminService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 分配用户角色
     */
    @Operation(summary = "分配用户角色", description = "为用户分配角色，body 传角色 ID 数组（标准 UUID 字符串）（需 user:assignRole 权限）")
    @SaCheckPermission("user:assignRole")
    @PutMapping("users/{id}/roles")
    Result<Void> assignUserRoles(@PathVariable @Parameter(description = "用户 ID") String id, @RequestBody List<UUID> roleIds) {
        adminService.assignUserRoles(id, roleIds);
        return Result.success(null);
    }

    /**
     * 角色列表
     */
    @Operation(summary = "角色列表", description = "查询全部角色（需 role:list 权限）")
    @SaCheckPermission("role:list")
    @GetMapping("roles")
    Result<List<Role>> listRoles() {
        return Result.success(adminService.listRoles());
    }

    /**
     * 新增角色
     */
    @Operation(summary = "新增角色", description = "创建角色（需 role:create 权限）")
    @SaCheckPermission("role:create")
    @PostMapping("roles")
    Result<Void> createRole(@RequestBody Role body) {
        adminService.createRole(body);
        return Result.success(null);
    }

    /**
     * 分配角色权限
     */
    @Operation(summary = "分配角色权限", description = "为角色分配权限点（需 role:assignPermission 权限）")
    @SaCheckPermission("role:assignPermission")
    @PutMapping("roles/{id}/permissions")
    Result<Void> assignRolePermissions(@PathVariable @Parameter(description = "角色 ID") String id, @RequestBody List<UUID> permissionIds) {
        adminService.assignRolePermissions(id, permissionIds);
        return Result.success(null);
    }

    // ==================== 权限 ====================

    /**
     * 权限列表
     */
    @Operation(summary = "权限列表", description = "查询全部权限点（需 role:list 权限）")
    @SaCheckPermission("role:list")
    @GetMapping("permissions")
    Result<List<Permission>> listPermissions() {
        return Result.success(adminService.listPermissions());
    }

    // ==================== 反馈 ====================

    /**
     * 反馈列表（分页）
     */
    @Operation(summary = "反馈列表", description = "分页查询用户反馈，支持关键词（称呼/联系方式/内容）筛选（需 feedback:list 权限）")
    @SaCheckPermission("feedback:list")
    @GetMapping("feedback")
    Result<EasyPageResult<Feedback>> listFeedback(
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键词（称呼/联系方式/内容模糊匹配）") String keyword,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return Result.success(adminService.listFeedback(keyword, pageable));
    }

    // ==================== 浏览量 ====================

    /**
     * 批量查询浏览量
     */
    @Operation(summary = "批量查询浏览量", description = "按内容类型与 ID 列表批量查询浏览量（需登录），返回 {id: count} 映射")
    @SaCheckLogin
    @GetMapping("view-counts")
    Result<Map<String, Long>> getViewCounts(
            @RequestParam @Parameter(description = "内容类型：ethnic / festival / art / topic") String type,
            @RequestParam @Parameter(description = "内容 ID 列表（逗号分隔）") List<String> ids
    ) {
        return Result.success(adminService.getViewCounts(type, ids));
    }

    // ==================== 统计 ====================

    /**
     * 统计总览
     */
    @Operation(summary = "统计总览", description = "访问 / 互动总览统计（需 stats:view 权限）")
    @SaCheckRole("super_admin")
    @GetMapping("stats/overview")
    Result<StatsOverviewResource> statsOverview() {
        return Result.success(adminService.statsOverview());
    }

    /**
     * 内容维度统计
     */
    @Operation(summary = "内容维度统计", description = "按内容维度的统计报表（需 stats:view 权限）")
    @SaCheckRole("super_admin")
    @GetMapping("stats/content")
    Result<List<ContentStatsResource>> statsContent() {
        return Result.success(adminService.statsContent());
    }
}
