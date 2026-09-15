package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionSubscriptionProxy;
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
 * 社区订阅（帖子 / 板块 / 用户 的通知强度）
 * level：all 全部通知 / mention 仅被 @ 时通知 / off 免打扰
 *
 * @author cz
 */
@Table(value = "discussion_subscription")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionSubscription implements ProxyEntityAvailable<DiscussionSubscription, DiscussionSubscriptionProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    /** topic / board / user */
    public String targetType;
    public UUID targetId;
    /** all / mention / off */
    public String level;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
