package com.czdr.work.service;

/**
 * 社区操作限流（Redis 计数窗口）
 *
 * @author cz
 */
public interface RateLimitService {

    /**
     * 检查并消耗一次配额，超限直接抛 RATE_LIMITED 业务异常
     *
     * @param action       动作标识（post / reply / report / upload ...）
     * @param userId       用户
     * @param limit        窗口内允许次数
     * @param windowSeconds 窗口长度（秒）
     * @param message      超限提示文案
     */
    void consume(String action, String userId, int limit, long windowSeconds, String message);
}
