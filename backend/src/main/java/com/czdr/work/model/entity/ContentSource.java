package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ContentSourceProxy;
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
 * 内容来源（方向 C-1：可溯源）
 *
 * <p>站内数据的权威出处（国家民委、国家统计局、文化和旅游部、Wikimedia Commons 等）。
 * 单独成表以便跨内容复用，并由 {@link ContentSourceLink} 与具体内容关联。</p>
 *
 * @author cz
 */
@Table(value = "content_source")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ContentSource implements ProxyEntityAvailable<ContentSource, ContentSourceProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String name;
    public String publisher;
    public String publisherShort;
    public String documentTitle;
    public String url;
    /** official / academic / open / other */
    public String sourceType;
    /** scrape / ocr / manual / api */
    public String collectMethod;
    public String remark;
    public Integer orderNum;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
