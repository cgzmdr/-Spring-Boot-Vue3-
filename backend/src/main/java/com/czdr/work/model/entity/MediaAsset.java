package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.MediaAssetProxy;
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
@Table(value = "media_asset")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class MediaAsset implements ProxyEntityAvailable<MediaAsset, MediaAssetProxy> {
    public UUID id;
    public String ownerType;
    public UUID ownerId;
    public String mediaType;
    public String url;
    public String title;
    public Integer sortOrder;
    public LocalDateTime createdAt;
}
