package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.FoodProxy;
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
 */
@Table(value = "food")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Food implements ProxyEntityAvailable<Food, FoodProxy> {
    public UUID id;
    public UUID ethnicGroupId;
    public String name;
    public String nameEn;
    public String description;
    /** 发展沿革 */
    public String origin;
    public String image;
    public Integer orderNum;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
