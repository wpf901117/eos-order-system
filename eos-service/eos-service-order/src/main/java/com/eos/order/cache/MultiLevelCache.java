package com.eos.order.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存管理器
 *
 * <p>实现 L1（Caffeine）+ L2（Redis）+ L3（数据库）三级缓存架构。</p>
 *
 * <p><strong>缓存层级：</strong></p>
 * <ul>
 *   <li>L1: Caffeine 本地缓存 - 毫秒级，容量 10000，过期时间 5 分钟</li>
 *   <li>L2: Redis 分布式缓存 - 秒级，过期时间 30 分钟</li>
 *   <li>L3: 数据库 - 兜底查询</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @Autowired
 * private MultiLevelCache cache;
 * 
 * ProductVO product = cache.get(
 *     "product:" + productId,
 *     ProductVO.class,
 *     () -> productMapper.selectById(productId)  // 数据库查询
 * );
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class MultiLevelCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** L1 缓存：Caffeine 本地缓存 */
    private Cache<String, Object> localCache;

    /** L1 缓存配置 */
    private static final int L1_MAX_SIZE = 10000;
    private static final int L1_EXPIRE_MINUTES = 5;

    /** L2 缓存配置 */
    private static final int L2_EXPIRE_MINUTES = 30;

    @PostConstruct
    public void init() {
        // 初始化 Caffeine 缓存
        localCache = Caffeine.newBuilder()
                .maximumSize(L1_MAX_SIZE)
                .expireAfterWrite(L1_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .recordStats()  // 开启统计
                .build();

        log.info("[多级缓存] L1 缓存初始化完成，最大容量: {}, 过期时间: {} 分钟", 
                 L1_MAX_SIZE, L1_EXPIRE_MINUTES);
    }

    /**
     * 获取缓存数据（三级缓存）
     *
     * @param key 缓存键
     * @param clazz 返回类型
     * @param loader 数据加载器（数据库查询）
     * @return 缓存数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz, Supplier<T> loader) {
        long startTime = System.currentTimeMillis();

        // L1: 本地缓存
        T value = (T) localCache.getIfPresent(key);
        if (value != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[L1 缓存命中] key={}, 耗时={}ms", key, duration);
            return value;
        }

        // L2: Redis 缓存
        value = (T) redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 回填 L1 缓存
            localCache.put(key, value);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[L2 缓存命中] key={}, 耗时={}ms", key, duration);
            return value;
        }

        // L3: 数据库查询
        value = loader.get();
        if (value != null) {
            // 写入 L2 缓存
            redisTemplate.opsForValue().set(key, value, L2_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 写入 L1 缓存
            localCache.put(key, value);
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[L3 数据库查询] key={}, 耗时={}ms", key, duration);
        } else {
            log.debug("[缓存未命中] key={}", key);
        }

        return value;
    }

    /**
     * 删除缓存（同时删除 L1 和 L2）
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        // 删除 L1 缓存
        localCache.invalidate(key);
        
        // 删除 L2 缓存
        redisTemplate.delete(key);
        
        log.debug("[缓存失效] key={}", key);
    }

    /**
     * 批量删除缓存（支持通配符）
     *
     * @param pattern 匹配模式，如 "product:*"
     */
    public void evictPattern(String pattern) {
        // 删除 L2 缓存
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
        
        // L1 缓存会在过期时自动清理，或者可以手动清理
        localCache.asMap().keySet().removeIf(key -> matchesPattern(key, pattern));
        
        log.debug("[批量缓存失效] pattern={}", pattern);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        localCache.invalidateAll();
        log.info("[清空 L1 缓存]");
        
        // Redis 全量删除需谨慎，生产环境建议使用命名空间
        // redisTemplate.getConnectionFactory().getConnection().flushDb();
        log.warn("[清空 L2 缓存] 已跳过，请手动执行");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public String getStats() {
        return String.format(
            "L1 缓存统计 - 命中率: %.2f%%, 请求数: %d, 命中数: %d, 未命中数: %d, 当前大小: %d",
            localCache.stats().hitRate() * 100,
            localCache.stats().requestCount(),
            localCache.stats().hitCount(),
            localCache.stats().missCount(),
            localCache.estimatedSize()
        );
    }

    /**
     * 简单的通配符匹配
     *
     * @param str 字符串
     * @param pattern 模式（支持 *）
     * @return 是否匹配
     */
    private boolean matchesPattern(String str, String pattern) {
        String regex = pattern.replace("*", ".*");
        return str.matches(regex);
    }
}
