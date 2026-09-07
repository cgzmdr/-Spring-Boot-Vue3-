package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.EthnicCustomProxy;
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
@Table(value = "ethnic_custom")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class EthnicCustom implements ProxyEntityAvailable<EthnicCustom, EthnicCustomProxy> {
    public UUID id;
    public UUID ethnicGroupId;
    public String category;
    public String title;
    public String content;
    public String image;
    public Integer orderNum;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
