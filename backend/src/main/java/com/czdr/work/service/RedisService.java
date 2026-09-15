package com.czdr.work.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void setString(String key, Object value, long timeout, TimeUnit unit);
    Object getString(String key);
    void setHash(String key, String hashKey, Object value);
    Object getHash(String key, String hashKey);
    void setList(String key, Object value);
    void setSet(String key, Object... values);
    Boolean delete(String key);

    /**
     * 自增计数（限流 / 未读计数等）：首次写入时设置过期时间，返回自增后的值。
     */
    long increment(String key, long timeoutSeconds);
}
