package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.SensitiveWordProxy;
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
 * 本地敏感词库（纯本地匹配，不依赖第三方内容安全服务）
 *
 * @author cz
 */
@Table(value = "sensitive_word")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWord implements ProxyEntityAvailable<SensitiveWord, SensitiveWordProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String word;
    public String locale;
    /** block 进待审队列 / watch 仅打标 / replace 替换为 * */
    public String level;
    public Boolean enabled;
    public String remark;
    public LocalDateTime createdAt;
}
