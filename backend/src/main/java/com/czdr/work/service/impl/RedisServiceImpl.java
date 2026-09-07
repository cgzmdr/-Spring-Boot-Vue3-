package com.czdr.work.service.impl;

import com.czdr.work.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    @Override
    public void setString(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public Object getString(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setHash(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public Object getHash(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public void setList(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public void setSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
}
