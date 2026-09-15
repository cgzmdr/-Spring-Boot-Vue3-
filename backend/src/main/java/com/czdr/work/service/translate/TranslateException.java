package com.czdr.work.service.translate;

/**
 * 翻译失败（自建服务不可用 / 超时 / 无词表命中）。
 * 上层捕获后降级为「仅原文」，不向前端抛错。
 *
 * @author cz
 */
public class TranslateException extends RuntimeException {

    public TranslateException(String message) {
        super(message);
    }

    public TranslateException(String message, Throwable cause) {
        super(message, cause);
    }
}
