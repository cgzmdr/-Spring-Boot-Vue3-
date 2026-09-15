package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionModerationLogProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 讨论区审核操作日志（合规审计：谁、何时、依据什么处置了哪条内容）
 *
 * @author cz
 */
@Table(value = "discussion_moderation_log")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionModerationLog implements ProxyEntityAvailable<DiscussionModerationLog, DiscussionModerationLogProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String targetType;
    public UUID targetId;
    public String action;
    public UUID operatorId;
    public String reason;
    public String snapshot;
    public LocalDateTime createdAt;
}
