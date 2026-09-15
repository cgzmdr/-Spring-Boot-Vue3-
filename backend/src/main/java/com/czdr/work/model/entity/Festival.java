package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.FestivalProxy;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "festival")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Festival implements ProxyEntityAvailable<Festival, FestivalProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String slug;
    public UUID ethnicGroupId;
    public String name;
    public String nameEn;
    public String type;
    public LocalDate solarDate;
    public String lunarDate;
    public String origin;
    public String description;
    /** 英文正文（方向 C-3）；为空时前端回退显示中文 description */
    public String descriptionEn;
    /** 英文正文来源：machine 机器翻译 / reviewed 人工校对 / manual 后台录入 */
    public String descriptionEnSource;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String customs;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String images;
    public String coverImage;
    public String status;
    public Integer orderNum;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
