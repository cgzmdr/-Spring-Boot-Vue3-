package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.PermissionProxy;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "permission")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Permission implements ProxyEntityAvailable<Permission, PermissionProxy> {
    public UUID id;
    public String code;
    public String name;
    public String description;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
