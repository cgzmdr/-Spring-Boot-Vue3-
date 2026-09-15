package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.UserInterestProxy;
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
 * 用户兴趣标签（方向 D·V16）—— 推荐系统的**显式信号**，用于冷启动。
 *
 * @author cz
 */
@Table(value = "user_interest")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class UserInterest implements ProxyEntityAvailable<UserInterest, UserInterestProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    public UUID tagId;
    /** 权重：用户标记「特别关注」时为 2，普通为 1 */
    public Integer weight;
    public LocalDateTime createdAt;
}
