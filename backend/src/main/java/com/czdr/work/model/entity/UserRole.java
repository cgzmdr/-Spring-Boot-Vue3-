package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.UserRoleProxy;
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
@Table(value = "user_role")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class UserRole implements ProxyEntityAvailable<UserRole,UserRoleProxy> {
    @Column(primaryKey = true)
    public UUID userId;
    @Column(primaryKey = true)
    public UUID roleId;
}
