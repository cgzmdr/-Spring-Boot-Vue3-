package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionBoardProxy;
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
 * 讨论区板块
 *
 * @author cz
 */
@Table(value = "discussion_board")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionBoard implements ProxyEntityAvailable<DiscussionBoard, DiscussionBoardProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String slug;
    public String name;
    public String nameEn;
    public String description;
    public String icon;
    public String themeColor;
    public Integer orderNum;
    /** active / hidden */
    public String status;
    /** post_then_review 先发后审 / review_then_post 先审后发 */
    public String postPolicy;
    public Integer minTrustLevel;
    public Integer topicCount;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
