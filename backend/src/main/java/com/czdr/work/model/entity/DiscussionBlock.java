package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionBlockProxy;
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
 * 拉黑关系（单向）：拉黑后双方不能互发私信，并解除已有互相关注
 *
 * @author cz
 */
@Table(value = "discussion_block")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionBlock implements ProxyEntityAvailable<DiscussionBlock, DiscussionBlockProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    public UUID blockedUserId;
    public LocalDateTime createdAt;
}
