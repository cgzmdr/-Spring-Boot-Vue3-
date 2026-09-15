package com.czdr.work.service.translate;

/**
 * 机器翻译提供方抽象。实现必须是「可失败」的：任何异常都向上抛 {@link TranslateException}，
 * 由上层降级为「仅显示原文」，绝不阻塞页面。
 *
 * @author cz
 */
public interface TranslateProvider {

    /** 提供方标识（落库到 discussion_translation.provider，并回传给前端做标注） */
    String name();

    /**
     * 翻译一段文本。
     *
     * @param text         原文（纯文本，可能含换行）
     * @param sourceLocale 原文语言（zh / en ...，可能为空）
     * @param targetLocale 目标语言（zh / en ...）
     * @return 译文；无法翻译时抛 {@link TranslateException}
     */
    String translate(String text, String sourceLocale, String targetLocale);
}
