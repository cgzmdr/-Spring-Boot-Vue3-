package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.InterestTagProxy;
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
 * 兴趣标签字典（方向 D·V16）。
 *
 * @author cz
 */
@Table(value = "interest_tag")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class InterestTag implements ProxyEntityAvailable<InterestTag, InterestTagProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 维度：ethnic 民族 / region 地域 / type 内容类型 / topic 主题 */
    public String dimension;
    public String name;
    public String nameEn;
    public String description;
    public String color;
    public Boolean enabled;
    public Integer orderNum;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
