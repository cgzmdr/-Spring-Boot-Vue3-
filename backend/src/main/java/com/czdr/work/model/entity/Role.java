package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.RoleProxy;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "role")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Role implements ProxyEntityAvailable<Role, RoleProxy> {
    public UUID id;
    public String code;
    public String name;
    public String description;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @Navigate(
            value = RelationTypeEnum.ManyToMany,
            mappingClass = RolePermission.class,
            selfMappingProperty = "roleId",
            targetMappingProperty = "permissionId",
            selfProperty = "id",
            targetProperty = "id"
    )
    public List<Permission> permissions;
}
