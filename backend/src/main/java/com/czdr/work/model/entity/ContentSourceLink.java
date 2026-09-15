package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ContentSourceLinkProxy;
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
 * 内容 ↔ 来源 关联（多对多）
 *
 * @author cz
 */
@Table(value = "content_source_link")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ContentSourceLink implements ProxyEntityAvailable<ContentSourceLink, ContentSourceLinkProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** ethnic / festival / art / food / topic / person / area / sport */
    public String targetType;
    public UUID targetId;
    public UUID sourceId;
    /** 该来源在该条内容上的具体说明 */
    public String note;
    public LocalDateTime createdAt;
}
