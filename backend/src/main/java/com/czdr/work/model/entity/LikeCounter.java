package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.LikeCounterProxy;
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
 * @author cz
 */
@Table(value = "like_counter")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class LikeCounter implements ProxyEntityAvailable<LikeCounter, LikeCounterProxy> {
    @Column(primaryKey = true)
    public String entryType;
    @Column(primaryKey = true)
    public UUID entryId;
    public Long count;
    public LocalDateTime updatedAt;
}
