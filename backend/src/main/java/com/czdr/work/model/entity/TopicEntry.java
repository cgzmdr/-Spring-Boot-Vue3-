package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.TopicEntryProxy;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "topic_entry")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class TopicEntry implements ProxyEntityAvailable<TopicEntry, TopicEntryProxy> {
    public UUID id;
    public UUID topicId;
    public String entryType;
    public UUID entryId;
    public Integer sortOrder;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "topicId", targetProperty = "id")
    public Topic topic;
}
