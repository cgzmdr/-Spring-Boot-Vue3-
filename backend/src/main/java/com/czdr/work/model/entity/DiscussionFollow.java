package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionFollowProxy;
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
 * 社区关注关系（单向；互相关注即成为好友，可互发私信）
 *
 * @author cz
 */
@Table(value = "discussion_follow")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionFollow implements ProxyEntityAvailable<DiscussionFollow, DiscussionFollowProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    public UUID followUserId;
    public LocalDateTime createdAt;
}
