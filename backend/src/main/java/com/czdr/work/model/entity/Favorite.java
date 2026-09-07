package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.FavoriteProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 * 收藏表
 */
@Table(value = "favorite")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Favorite implements ProxyEntityAvailable<Favorite, FavoriteProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    public String entryType;
    public UUID entryId;
    public LocalDateTime createdAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "userId", targetProperty = "id")
    public UserAuth user;
}
