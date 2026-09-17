package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
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
import java.util.List;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "ethnic_group")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class EthnicGroup implements ProxyEntityAvailable<EthnicGroup, EthnicGroupProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String slug;
    public String name;
    public String nameEn;
    public String selfName;
    public String pinyin;
    public Long population;
    public String languageFamily;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String region;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String languages;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String scripts;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String religion;
    public String summary;
    public String summaryEn;
    public String description;
    public String descriptionEn;
    public String coverImage;
    public String themeColor;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String tags;
    public String status;
    /** 内容版本号：每提交一次审批 +1，审批/审查意见按该版本归档 */
    public Integer contentVersion;
    public Integer orderNum;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "ethnicGroupId")
    public List<EthnicCustom> customs;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "ethnicGroupId")
    public List<EthnicLocation> locations;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "ethnicGroupId")
    public List<Food> foods;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "ethnicGroupId")
    public List<Festival> festivals;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "ethnicGroupId")
    public List<Art> arts;
}
