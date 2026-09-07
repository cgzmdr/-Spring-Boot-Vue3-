package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.EthnicLocationProxy;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "ethnic_location")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class EthnicLocation implements ProxyEntityAvailable<EthnicLocation, EthnicLocationProxy> {
    public UUID id;
    public UUID ethnicGroupId;
    public String province;
    public String city;
    public BigDecimal longitude;
    public BigDecimal latitude;
    public String description;
    public Integer orderNum;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
