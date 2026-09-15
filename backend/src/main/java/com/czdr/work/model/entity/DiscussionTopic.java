package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.DiscussionTopicProxy;
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
 * 讨论区帖子
 *
 * @author cz
 */
@Table(value = "discussion_topic")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionTopic implements ProxyEntityAvailable<DiscussionTopic, DiscussionTopicProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID boardId;
    public UUID authorId;
    public String title;
    /** 纯文本正文（保留换行，前端安全渲染） */
    public String content;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String images;
    public String lang;
    /** published / pending / hidden / rejected / deleted */
    public String status;
    public Boolean pinned;
    public Boolean featured;
    public Boolean locked;
    public String linkedType;
    public UUID linkedId;
    public Integer replyCount;
    public Integer likeCount;
    public Integer favoriteCount;
    public Long viewCount;
    public Integer floorCount;
    public LocalDateTime lastReplyAt;
    public UUID lastReplyUserId;
    /** none / watch / block（敏感词命中等级） */
    public String riskLevel;
    public String hitWords;
    public LocalDateTime editedAt;
    public Integer editCount;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "boardId", targetProperty = "id")
    public DiscussionBoard board;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "authorId", targetProperty = "id")
    public UserAuth author;
}
