package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.TranslateGlossaryProxy;
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
 * 翻译词表（术语 → 译文），glossary 提供方的数据源；后台可维护。
 *
 * @author cz
 */
@Table(value = "translate_glossary")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class TranslateGlossary implements ProxyEntityAvailable<TranslateGlossary, TranslateGlossaryProxy> {

    @Column(primaryKey = true)
    public UUID id;
    /** 源语言（如 zh） */
    public String sourceLocale;
    /** 目标语言（如 en） */
    public String targetLocale;
    /** 术语（如 蒙古族 / Mongolian） */
    public String term;
    /** 译文（如 Mongolian / 蒙古族） */
    public String translation;
    public Boolean enabled;
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
