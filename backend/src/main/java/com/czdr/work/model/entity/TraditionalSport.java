package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.TraditionalSportProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 传统体育项目（B-5）
 *
 * <p>以全国少数民族传统体育运动会竞赛项目为主体。
 * 数据来源：国家民委、国家体育总局《全国少数民族传统体育运动会总规程》
 * 与湖北省民宗委《少数民族传统体育项目》简介。</p>
 *
 * @author cz
 */
@Table(value = "traditional_sport")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class TraditionalSport implements ProxyEntityAvailable<TraditionalSport, TraditionalSportProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String name;
    /** ball / water / strength / accuracy / speed / martial / equestrian / gymnastics / swing */
    public String category;
    /** 起源与主要流行的民族（JSONB 数组文本） */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String ethnicOrigins;
    public String description;
    public String equipment;
    public String venue;
    public String teamSize;
    public Integer firstEventYear;
    /** 多子项项目的子项名称（JSONB 数组文本） */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String subEvents;
    public String heritageLink;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
