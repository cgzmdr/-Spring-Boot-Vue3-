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
}
