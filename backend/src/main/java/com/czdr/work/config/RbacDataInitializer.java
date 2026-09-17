package com.czdr.work.config;

import com.czdr.work.model.entity.Permission;
import com.czdr.work.model.entity.Role;
import com.czdr.work.model.entity.RolePermission;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.entity.UserRole;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacDataInitializer implements ApplicationRunner {

    private final EasyEntityQuery entityQuery;
    private final AesCbcEncryptor aesCbcEncryptor;

    @Value("${app.admin.account:admin}")
    private String adminAccount;

    @Value("${app.admin.password:123456}")
    private String adminPassword;

    private static final Map<String, String> PERMISSIONS = permissionMap();

    private static final Map<String, String> ROLES = roleMap();

    private static final Map<String, List<String>> ROLE_PERMISSIONS = rolePermissionMap();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            initPermissions();
            initRoles();
            initRolePermissions();
            initSuperAdmin();
        } catch (Exception e) {
            throw new RuntimeException("RBAC 初始化失败: " + e.getMessage(), e);
        }
    }

    private void initPermissions() {
        PERMISSIONS.forEach((code, name) -> {
            if (queryPermissionByCode(code) == null) {
                Permission permission = new Permission();
                permission.setId(UUID.randomUUID());
                permission.setCode(code);
                permission.setName(name);
                permission.setCreatedAt(LocalDateTime.now());
                permission.setUpdatedAt(LocalDateTime.now());
                entityQuery.insertable(permission).executeRows();
            }
        });
    }

    private void initRoles() {
        ROLES.forEach((code, name) -> {
            if (queryRoleByCode(code) == null) {
                Role role = new Role();
                role.setId(UUID.randomUUID());
                role.setCode(code);
                role.setName(name);
                role.setCreatedAt(LocalDateTime.now());
                role.setUpdatedAt(LocalDateTime.now());
                entityQuery.insertable(role).executeRows();
            }
        });
    }

    private void initRolePermissions() {
        ROLE_PERMISSIONS.forEach((roleCode, permissionCodes) -> {
            Role role = queryRoleByCode(roleCode);
            if (role == null) {
                throw new IllegalStateException("角色不存在: " + roleCode);
            }
            for (String permissionCode : permissionCodes) {
                Permission permission = queryPermissionByCode(permissionCode);
                if (permission == null) {
                    throw new IllegalStateException("权限不存在: " + permissionCode);
                }
                boolean bound = entityQuery.queryable(RolePermission.class)
                        .where(rp -> {
                            rp.roleId().eq(role.getId());
                            rp.permissionId().eq(permission.getId());
                        })
                        .firstOrNull() != null;
                if (!bound) {
                    entityQuery.insertable(new RolePermission(role.getId(), permission.getId())).executeRows();
                }
            }
        });
    }

    private void initSuperAdmin() throws Exception {
        UserAuth admin = entityQuery.queryable(UserAuth.class)
                .where(u -> u.account().eq(adminAccount))
                .firstOrNull();
        if (admin != null) {
            return;
        }
        UUID adminId = UUID.randomUUID();
        String passwordHash = aesCbcEncryptor.encrypt(adminPassword);
        UserAuth adminUser = new UserAuth();
        adminUser.setId(adminId);
        adminUser.setAccount(adminAccount);
        adminUser.setPasswordHash(passwordHash);
        adminUser.setNickname("超级管理员");
        adminUser.setStatus("active");
        adminUser.setCreatedAt(LocalDate.now());
        adminUser.setUpdatedAt(LocalDate.now());
        entityQuery.insertable(adminUser).executeRows();
        Role superAdminRole = queryRoleByCode("super_admin");
        if (superAdminRole == null) {
            throw new IllegalStateException("角色不存在: super_admin");
        }
        entityQuery.insertable(new UserRole(adminId, superAdminRole.getId())).executeRows();
    }

    private Permission queryPermissionByCode(String code) {
        return entityQuery.queryable(Permission.class)
                .where(p -> p.code().eq(code))
                .firstOrNull();
    }

    private Role queryRoleByCode(String code) {
        return entityQuery.queryable(Role.class)
                .where(r -> r.code().eq(code))
                .firstOrNull();
    }

    private static Map<String, String> permissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("*", "超级权限");
        map.put("ethnic:create", "新增民族");
        map.put("ethnic:update", "编辑民族");
        map.put("ethnic:delete", "删除民族");
        map.put("ethnic:list", "民族列表");
        map.put("ethnic:view", "民族详情");
        map.put("festival:create", "新增节日");
        map.put("festival:update", "编辑节日");
        map.put("festival:delete", "删除节日");
        map.put("festival:list", "节日列表");
        map.put("art:create", "新增艺术");
        map.put("art:update", "编辑艺术");
        map.put("art:delete", "删除艺术");
        map.put("art:list", "艺术列表");
        map.put("topic:create", "新增专题");
        map.put("topic:update", "编辑专题");
        map.put("topic:delete", "删除专题");
        map.put("topic:list", "专题列表");
        map.put("form:create", "新建表单配置");
        map.put("form:update", "编辑表单配置");
        map.put("form:delete", "删除表单配置");
        map.put("form:list", "表单配置列表");
        map.put("review:list", "审核列表");
        map.put("review:approve", "审核通过");
        map.put("review:reject", "审核驳回");
        // 内容审批工作流（Camunda 8）：内容管理员审查与下线、流程建模与部署
        map.put("review:inspect", "内容审查（审查意见/通过）");
        map.put("review:offline", "审查发现问题并暂时下线");
        map.put("workflow:list", "工作流定义查看");
        map.put("workflow:save", "工作流建模与保存");
        map.put("workflow:deploy", "工作流部署到引擎");
        map.put("user:list", "用户列表");
        map.put("user:view", "用户详情");
        map.put("user:create", "新建用户");
        map.put("user:update", "编辑用户");
        map.put("user:delete", "删除用户");
        map.put("user:assignRole", "分配角色");
        map.put("feedback:list", "查看反馈");
        map.put("role:list", "角色列表");
        map.put("role:create", "新增角色");
        map.put("role:assignPermission", "分配权限");
        map.put("stats:view", "统计查看");
        map.put("search:hot:update", "热门词维护");
        map.put("content:like", "点赞");
        map.put("content:favorite", "收藏");
        map.put("share:create", "分享");
        // 讨论区（社区）
        map.put("discussion:review", "讨论区审核与举报处理");
        map.put("discussion:user:mute", "讨论区用户禁言");
        map.put("discussion:board", "讨论区板块管理");
        // 站内公告（OA 群发）与翻译词表
        map.put("system:broadcast", "站内公告群发");
        map.put("translate:glossary", "翻译词表维护");
        // B 系列新增内容：人物档案 / 自治地方 / 传统体育（按业务域归入「内容运营」）
        map.put("person:list", "人物档案列表");
        map.put("person:create", "新增人物档案");
        map.put("person:update", "编辑人物档案");
        map.put("person:delete", "删除人物档案");
        map.put("area:list", "自治地方列表");
        map.put("area:create", "新增自治地方");
        map.put("area:update", "编辑自治地方");
        map.put("area:delete", "删除自治地方");
        map.put("sport:list", "传统体育列表");
        map.put("sport:create", "新增传统体育项目");
        map.put("sport:update", "编辑传统体育项目");
        map.put("sport:delete", "删除传统体育项目");
        // 方向 C：内容来源（可溯源）
        map.put("source:list", "内容来源列表");
        map.put("source:create", "新增内容来源");
        map.put("source:update", "编辑内容来源");
        map.put("source:delete", "删除内容来源");
        // 方向 C-4：图片版权署名
        map.put("credit:list", "图片署名列表");
        map.put("credit:update", "核实/编辑图片署名");
        return map;
    }

    private static Map<String, String> roleMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("super_admin", "超级管理员");
        map.put("content_admin", "内容管理员");
        map.put("editor", "内容编辑");
        map.put("reviewer", "审核员");
        map.put("operator", "运营");
        map.put("user", "普通用户");
        return map;
    }

    private static Map<String, List<String>> rolePermissionMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("super_admin", List.copyOf(PERMISSIONS.keySet()));
        map.put("content_admin", List.of(
                "ethnic:create", "ethnic:update", "ethnic:delete", "ethnic:list", "ethnic:view",
                "festival:create", "festival:update", "festival:delete", "festival:list",
                "art:create", "art:update", "art:delete", "art:list",
                "translate:glossary",
                "topic:create", "topic:update", "topic:delete", "topic:list",
                "form:create", "form:update", "form:delete", "form:list",
                "person:list", "person:create", "person:update", "person:delete",
                "area:list", "area:create", "area:update", "area:delete",
                "sport:list", "sport:create", "sport:update", "sport:delete",
                "source:list", "source:create", "source:update", "source:delete",
                "credit:list", "credit:update",
                // 内容管理员：可审查上线内容、发现问题下线、查看审批过程、维护流程模型
                "review:list", "review:inspect", "review:offline",
                "workflow:list", "workflow:save", "workflow:deploy"));
        map.put("editor", List.of(
                "ethnic:create", "ethnic:update",
                "festival:create", "festival:update",
                "art:create", "art:update",
                "person:list", "person:update",
                "area:list", "area:update",
                "sport:list", "sport:update",
                "source:list", "source:update",
                "credit:list", "credit:update",
                // 内容编辑：需要看到前序审批/审查意见，才能按意见修改
                "review:list"));
        map.put("reviewer", List.of("review:list", "review:approve", "review:reject",
                "discussion:review", "workflow:list"));
        map.put("operator", List.of("topic:update", "search:hot:update", "stats:view", "discussion:review"));
        map.put("user", List.of("content:like", "content:favorite", "share:create"));
        return map;
    }
}