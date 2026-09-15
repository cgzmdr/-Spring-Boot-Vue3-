package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 基于 Redis 计数的限流实现：Redis 不可用时放行（不因限流组件故障阻断正常发帖）。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisService redisService;

    @Override
    public void consume(String action, String userId, int limit, long windowSeconds, String message) {
        if (action == null || userId == null || limit <= 0) {
            return;
        }
        String key = "rl:%s:%s".formatted(action, userId);
        long current;
        try {
            current = redisService.increment(key, windowSeconds);
        } catch (Exception e) {
            log.warn("限流计数失败（放行）：{}", e.getMessage());
            return;
        }
        if (current > limit) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, message);
        }
    }
}
