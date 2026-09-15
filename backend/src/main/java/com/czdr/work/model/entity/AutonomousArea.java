package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.AutonomousAreaProxy;
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
 * 民族自治地方（B-6）
 *
 * <p>5 个自治区 + 30 个自治州 + 120 个自治县/旗 = 155 个行政区划实体。
 * 与 {@code ethnic_group.region}（民族主要聚居的省级行政区，粗略线索）不同，
 * 本表是**具体的自治地方**。</p>
 *
 * @author cz
 */
@Table(value = "autonomous_area")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class AutonomousArea implements ProxyEntityAvailable<AutonomousArea, AutonomousAreaProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String name;
    /** autonomous_region / autonomous_prefecture / autonomous_county */
    public String level;
    /** 冠名的自治民族（JSONB 数组文本） */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String ethnicGroups;
    public String province;
    public Integer establishedYear;
    public String seat;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
