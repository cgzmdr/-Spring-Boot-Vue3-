package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ImageCreditProxy;
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
 * 图片版权署名（方向 C-4）
 *
 * <p>{@code creditStatus} 是本表的核心：{@code verified} 已核实（有作者与许可）/
 * {@code unverified} 来源待核 / {@code original} 原创或无需署名。
 * 页面对待核图片显示「来源待核」，而不是空白或假名 —— 如实反映核实进度。</p>
 *
 * @author cz
 */
@Table(value = "image_credit")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ImageCredit implements ProxyEntityAvailable<ImageCredit, ImageCreditProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String imagePath;
    public String targetType;
    public UUID targetId;
    public String caption;
    /** verified / unverified / original */
    public String creditStatus;
    public String author;
    public String license;
    public String licenseUrl;
    public String sourceUrl;
    public String sourceSite;
    public Boolean attributionRequired;
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
