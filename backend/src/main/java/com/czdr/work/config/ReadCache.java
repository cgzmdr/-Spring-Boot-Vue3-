package com.czdr.work.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 进程内只读结果缓存（用于「目录型」公开接口）。
 *
 * <p><b>为什么不用 Redis</b>：生产环境 Redis 虽与应用同机，但仍要一次网络往返 +
 * 序列化/反序列化；而这里缓存的是「全量目录」这类体积大、读多写极少的数据
 * （民族 56 条、自治地方 155 条、人物 164 条）。放在进程内可做到零网络开销。
 * Redis 更适合跨实例共享的会话类数据。</p>
 *
 * <p><b>一致性策略</b>：「短 TTL + 主动失效」。后台修改内容时可调用
 * {@link #invalidateAll()} 立即清空；即使漏调，最多脏一个 TTL 周期，对目录页可接受。</p>
 *
 * <p><b>并发</b>：读路径无锁。命中即返回；未命中时用 {@code compute} 原子替换，
 * 保证同一 key 并发回源时不会叠加打爆数据库，也不会读到「半成品」条目。</p>
 *
 * @author cz
 */
@Component
public class ReadCache {

    private static final Logger log = LoggerFactory.getLogger(ReadCache.class);

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 缓存开关：默认开启，异常时可通过配置一键关闭，无需改代码 */
    @Value("${app.cache.enabled:true}")
    private boolean enabled;

    /** 存活时间（毫秒）。目录类数据变化少，60 秒兜底足够 */
    @Value("${app.cache.ttl-ms:60000}")
    private long ttlMs;

    /** 回源耗时超过该值就打日志，便于定位是哪个目录页拖慢了整体响应 */
    private static final long SLOW_LOAD_MS = 200;

    private record Entry(Object value, long expireAt) {
        boolean expired(long now) {
            return now > expireAt;
        }
    }

    /**
     * 读缓存，未命中或已过期则调用 {@code loader} 回源并写入。
     *
     * @param key    缓存键，需唯一标识「查询参数 + 数据版本」
     * @param loader 回源逻辑
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }
        Entry hit = store.get(key);
        if (hit != null && !hit.expired(System.currentTimeMillis())) {
            return (T) hit.value();
        }
        // 未命中 / 已过期：用 compute 原子地重新装载。
        // compute 在同一 key 上有锁语义，并发请求只会有一个真正执行 loader，
        // 其余线程等待后直接拿到新值，避免缓存击穿。
        Entry loaded = store.compute(key, (k, current) -> {
            if (current != null && !current.expired(System.currentTimeMillis())) {
                // 期间已被别的线程刷新过，直接复用
                return current;
            }
            long t0 = System.currentTimeMillis();
            Object value = loader.get();
            long cost = System.currentTimeMillis() - t0;
            if (cost > SLOW_LOAD_MS) {
                log.info("缓存回源 {} 耗时 {} ms", k, cost);
            }
            return new Entry(value, System.currentTimeMillis() + ttlMs);
        });
        return (T) loaded.value();
    }

    /** 清空全部缓存：内容后台发生写操作时调用 */
    public void invalidateAll() {
        int n = store.size();
        store.clear();
        if (n > 0) {
            log.info("已清空只读缓存，共 {} 项", n);
        }
    }

    /** 按前缀失效，例如内容更新后只清目录相关缓存 */
    public void invalidatePrefix(String prefix) {
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** 当前缓存条目数（便于运维观测） */
    public int size() {
        return store.size();
    }
}
