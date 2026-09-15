package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.NotificationProxy;
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
 * 站内通知（邮件通知复用同一行，emailed 标记是否已发信）
 *
 * @author cz
 */
@Table(value = "notification")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Notification implements ProxyEntityAvailable<Notification, NotificationProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    /** topic_reply / post_reply / mention / like / favorite / review_result / report_result / system */
    public String type;
    public UUID actorId;
    public String targetType;
    public UUID targetId;
    public String title;
    public String content;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String payload;
    public LocalDateTime readAt;
    public Boolean emailed;
    public LocalDateTime createdAt;
}
