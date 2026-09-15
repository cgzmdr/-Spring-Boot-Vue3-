package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.UserProfileProxy;
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
 * 用户兴趣画像（方向 D·V16）—— 由行为聚合而来，供推荐查询直接读取。
 *
 * <p>{@code confidence} 是**诚实性字段**：行为样本越少可信度越低，
 * 推荐接口据此如实告知前端「当前推荐主要基于内容相似度与热度」，
 * 而不是假装个性化已经很准。</p>
 *
 * @author cz
 */
@Table(value = "user_profile")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile implements ProxyEntityAvailable<UserProfile, UserProfileProxy> {
    @Column(primaryKey = true)
    public UUID userId;
    /** 兴趣民族 [{name,score}] */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String topEthnics;
    /** 兴趣分类 [{name,score}] */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String topCategories;
    /** 行为总量 */
    public Integer behaviorCount;
    /** 画像可信度 0~1 */
    public Double confidence;
    public LocalDateTime refreshedAt;
}
