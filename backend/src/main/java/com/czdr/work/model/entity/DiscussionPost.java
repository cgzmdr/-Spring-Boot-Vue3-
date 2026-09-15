package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.DiscussionPostProxy;
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
 * 讨论区楼层回复
 *
 * @author cz
 */
@Table(value = "discussion_post")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionPost implements ProxyEntityAvailable<DiscussionPost, DiscussionPostProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID topicId;
    public UUID parentId;
    public UUID authorId;
    public String content;
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String images;
    public String lang;
    /** published / pending / hidden / deleted */
    public String status;
    public Integer floorNo;
    public UUID quotePostId;
    public Integer likeCount;
    public Integer replyCount;
    public String riskLevel;
    public String hitWords;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "topicId", targetProperty = "id")
    public DiscussionTopic topic;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "authorId", targetProperty = "id")
    public UserAuth author;
}
