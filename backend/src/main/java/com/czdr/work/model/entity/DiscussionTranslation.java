package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionTranslationProxy;
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
 * 机器翻译译文缓存：按「原文指纹」失效，同一原文同一目标语言只译一次。
 *
 * @author cz
 */
@Table(value = "discussion_translation")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionTranslation implements ProxyEntityAvailable<DiscussionTranslation, DiscussionTranslationProxy> {

    @Column(primaryKey = true)
    public UUID id;
    /** topic 帖子 / post 楼层 / message 私信 / board 板块 */
    public String targetType;
    public UUID targetId;
    /** 目标语言（读者语言） */
    public String targetLocale;
    /** 原文语言 */
    public String sourceLocale;
    /** 原文指纹（sha256） */
    public String sourceHash;
    /** 原文摘要 */
    public String sourceExcerpt;
    public String content;
    /** libretranslate / ollama / glossary */
    public String provider;
    public LocalDateTime createdAt;
}
