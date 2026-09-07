package com.czdr.work.config;

import cn.dev33.satoken.stp.StpInterface;
import com.czdr.work.model.entity.Permission;
import com.czdr.work.model.entity.Role;
import com.czdr.work.model.entity.RolePermission;
import com.czdr.work.model.entity.UserRole;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final EasyEntityQuery entityQuery;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<UUID> roleIds = findRoleIds(loginId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<UUID> permissionIds = entityQuery.queryable(RolePermission.class)
                .where(rp -> rp.roleId().in(roleIds))
                .toList()
                .stream()
                .map(RolePermission::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return entityQuery.queryable(Permission.class)
                .where(p -> p.id().in(permissionIds))
                .toList()
                .stream()
                .map(Permission::getCode)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<UUID> roleIds = findRoleIds(loginId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return entityQuery.queryable(Role.class)
                .where(r -> r.id().in(roleIds))
                .toList()
                .stream()
                .map(Role::getCode)
                .distinct()
                .toList();
    }

    private List<UUID> findRoleIds(Object loginId) {
        try {
            UUID userId = UUID.fromString(String.valueOf(loginId));
            return entityQuery.queryable(UserRole.class)
                    .where(ur -> ur.userId().eq(userId))
                    .toList()
                    .stream()
                    .map(UserRole::getRoleId)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("查询用户角色失败", e);
        }
    }
}
