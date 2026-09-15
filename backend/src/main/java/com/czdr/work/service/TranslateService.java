package com.czdr.work.service;

import com.czdr.work.model.resource.TranslateStatusResource;
import com.czdr.work.model.resource.TranslationResource;

import java.util.UUID;

/**
 * 机器翻译（自建服务 + 译文缓存）。失败一律降级为「仅原文」，不抛错给页面。
 *
 * @author cz
 */
public interface TranslateService {

    /** 翻译能力状态（前端据此决定是否展示「译」按钮） */
    TranslateStatusResource status();

    /**
     * 按需翻译。
     *
     * @param targetType   topic / post / message / board
     * @param targetId     内容 ID
     * @param targetLocale 目标语言
     * @param scope        title / body / all（默认 all）
     * @param viewerId     当前登录用户（可为 null：游客也能看公开内容的翻译）
     * @return 翻译结果；不可用时 translated=false 并回退原文
     */
    TranslationResource translate(String targetType, UUID targetId, String targetLocale, String scope, UUID viewerId);
}
