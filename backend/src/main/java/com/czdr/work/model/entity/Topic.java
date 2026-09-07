package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.TopicProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "topic")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Topic implements ProxyEntityAvailable<Topic, TopicProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String slug;
    public String title;
    public String subtitle;
    public String description;
    public String coverImage;
    public String status;
    public Integer orderNum;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.OneToMany, selfProperty = "id", targetProperty = "topicId")
    public List<TopicEntry> entries;
}
