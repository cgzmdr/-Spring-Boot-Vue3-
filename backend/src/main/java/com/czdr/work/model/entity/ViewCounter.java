package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ViewCounterProxy;
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
 * 浏览量计数器（按内容类型 + 内容 ID 维度）
 *
 * @author cz
 */
@Table(value = "view_counter")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ViewCounter implements ProxyEntityAvailable<ViewCounter, ViewCounterProxy> {
    @Column(primaryKey = true)
    public String entryType;
    @Column(primaryKey = true)
    public UUID entryId;
    public Long count;
    public LocalDateTime updatedAt;
}
