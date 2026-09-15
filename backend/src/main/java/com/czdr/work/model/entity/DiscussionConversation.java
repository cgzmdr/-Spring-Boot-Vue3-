package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionConversationProxy;
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
 * 私信会话（两人唯一，user_a &lt; user_b）
 *
 * @author cz
 */
@Table(value = "discussion_conversation")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionConversation implements ProxyEntityAvailable<DiscussionConversation, DiscussionConversationProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userA;
    public UUID userB;
    public LocalDateTime lastMessageAt;
    public String lastPreview;
    public UUID lastSenderId;
    /** user_a 的未读数 */
    public Integer unreadA;
    /** user_b 的未读数 */
    public Integer unreadB;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
