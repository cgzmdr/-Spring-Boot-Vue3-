package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.RolePermissionProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "role_permission")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class RolePermission implements ProxyEntityAvailable<RolePermission, RolePermissionProxy> {
    @Column(primaryKey = true)
    public UUID roleId;
    @Column(primaryKey = true)
    public UUID permissionId;
}
