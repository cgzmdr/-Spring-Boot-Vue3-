package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "art")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Art implements ProxyEntityAvailable<Art, ArtProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String slug;
    public UUID ethnicGroupId;
    public String name;
    public String nameEn;
    public String category;
    public String intangibleHeritage;
    public String description;
    /** 英文正文（方向 C-3）；为空时前端回退显示中文 description */
    public String descriptionEn;
    /** 英文正文来源：machine 机器翻译 / reviewed 人工校对 / manual 后台录入 */
    public String descriptionEnSource;
    /** 发展沿革 */
    public String origin;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String inheritors;
    public String coverImage;
    public String status;
    /** 内容版本号：每提交一次审批 +1 */
    public Integer contentVersion;
    public Integer orderNum;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
