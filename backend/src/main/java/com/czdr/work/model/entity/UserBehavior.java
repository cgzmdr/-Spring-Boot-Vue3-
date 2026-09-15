package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.UserBehaviorProxy;
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
 * 用户行为明细（方向 D·V16）—— 推荐系统的**隐式信号**。
 *
 * <p>已有 view_counter / like_record / favorite 均为聚合计数，无法回答
 * 「谁在何时看了什么」，因此无法据此构建个人画像。本表记录带用户与时间的明细。</p>
 *
 * @author cz
 */
@Table(value = "user_behavior")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior implements ProxyEntityAvailable<UserBehavior, UserBehaviorProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID userId;
    /** view 浏览 / like 点赞 / favorite 收藏 / search 搜索 */
    public String action;
    public String targetType;
    public UUID targetId;
    /** 搜索行为的关键词（用于从搜索词反推兴趣） */
    public String keyword;
    /** 行为权重：view 1 / search 2 / like 3 / favorite 5 */
    public Integer weight;
    public LocalDateTime createdAt;
}
