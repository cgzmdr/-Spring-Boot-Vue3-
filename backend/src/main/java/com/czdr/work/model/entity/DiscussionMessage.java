package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.DiscussionMessageProxy;
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
 * 私信消息
 *
 * @author cz
 */
@Table(value = "discussion_message")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionMessage implements ProxyEntityAvailable<DiscussionMessage, DiscussionMessageProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID conversationId;
    public UUID senderId;
    public String content;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String images;
    public String lang;
    public LocalDateTime readAt;
    /** 是否已撤回（撤回后正文清空，保留占位） */
    public Boolean recalled;
    public LocalDateTime recalledAt;
    public LocalDateTime createdAt;
}
