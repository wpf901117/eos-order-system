package com.eos.order.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存一致性策略
 *
 * <p>提供多种缓存更新策略：</p>
 * <ul>
 *   <li>Cache-Aside Pattern（旁路缓存）- 推荐</li>
 *   <li>Read-Through Pattern（读穿透）</li>
 *   <li>Write-Through Pattern（写穿透）</li>
 *   <li>Write-Behind Pattern（异步写入）</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class CacheConsistencyStrategy {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认缓存过期时间（分钟）
     */
    private static final int DEFAULT_EXPIRE_MINUTES = 30;

    // ==================== Cache-Aside Pattern（旁路缓存）====================

    /**
     * Cache-Aside Pattern - 读操作
     *
     * <p><strong>流程：</strong></p>
     * <ol>
     *   <li>先读缓存</li>
     *   <li>缓存命中则返回</li>
     *   <li>缓存未命中则读数据库</li>
     *   <li>将数据写入缓存</li>
     * </ol>
     *
     * @param key 缓存 Key
     * @param dbLoader 数据库加载器
     * @return 数据
     */
    @SuppressWarnings("unchecked")
    public <T> T cacheAsideRead(String key, Supplier<T> dbLoader) {
        // 1. 读缓存
        T value = (T) redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("[Cache-Aside] 缓存命中，key={}", key);
            return value;
        }

        // 2. 读数据库
        log.debug("[Cache-Aside] 缓存未命中，查询数据库，key={}", key);
        value = dbLoader.get();

        // 3. 写入缓存
        if (value != null) {
            redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("[Cache-Aside] 写入缓存，key={}", key);
        }

        return value;
    }

    /**
     * Cache-Aside Pattern - 写操作
     *
     * <p><strong>流程：</strong></p>
     * <ol>
     *   <li>先更新数据库</li>
     *   <li>再删除缓存（不是更新）</li>
     * </ol>
     *
     * <p><strong>为什么删除而不是更新？</strong></p>
     * <ul>
     *   <li>避免并发问题（写 A → 写 B → 删 A → 读旧数据 → 写旧数据到缓存）</li>
     *   <li>懒加载策略，下次读取时再加载最新数据</li>
     * </ul>
     *
     * @param key 缓存 Key
     * @param dbUpdater 数据库更新器
     */
    public void cacheAsideWrite(String key, Runnable dbUpdater) {
        // 1. 更新数据库
        dbUpdater.run();
        log.debug("[Cache-Aside] 数据库更新成功，key={}", key);

        // 2. 删除缓存
        redisTemplate.delete(key);
        log.debug("[Cache-Aside] 删除缓存，key={}", key);
    }

    // ==================== Read-Through Pattern（读穿透）====================

    /**
     * Read-Through Pattern - 读操作
     *
     * <p><strong>特点：</strong></p>
     * <ul>
     *   <li>缓存层封装了数据库访问逻辑</li>
     *   <li>应用只与缓存交互</li>
     *   <li>适合读多写少的场景</li>
     * </ul>
     *
     * @param key 缓存 Key
     * @param dbLoader 数据库加载器
     * @return 数据
     */
    @SuppressWarnings("unchecked")
    public <T> T readThrough(String key, Supplier<T> dbLoader) {
        T value = (T) redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            // 缓存未命中，自动从数据库加载
            value = dbLoader.get();
            if (value != null) {
                redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("[Read-Through] 自动加载并缓存，key={}", key);
            }
        } else {
            log.debug("[Read-Through] 缓存命中，key={}", key);
        }

        return value;
    }

    // ==================== Write-Through Pattern（写穿透）====================

    /**
     * Write-Through Pattern - 写操作
     *
     * <p><strong>流程：</strong></p>
     * <ol>
     *   <li>同时更新缓存和数据库</li>
     *   <li>两者都成功才返回成功</li>
     * </ol>
     *
     * <p><strong>特点：</strong></p>
     * <ul>
     *   <li>保证缓存和数据库强一致性</li>
     *   <li>写性能较低</li>
     *   <li>适合对一致性要求高的场景</li>
     * </ul>
     *
     * @param key 缓存 Key
     * @param value 新值
     * @param dbUpdater 数据库更新器
     */
    public void writeThrough(String key, Object value, Runnable dbUpdater) {
        try {
            // 1. 更新数据库
            dbUpdater.run();
            
            // 2. 更新缓存
            redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.debug("[Write-Through] 同时更新数据库和缓存，key={}", key);

        } catch (Exception e) {
            log.error("[Write-Through] 更新失败，key={}", key, e);
            throw new RuntimeException("数据更新失败", e);
        }
    }

    // ==================== Write-Behind Pattern（异步写入）====================

    /**
     * Write-Behind Pattern - 异步写操作
     *
     * <p><strong>流程：</strong></p>
     * <ol>
     *   <li>先更新缓存</li>
     *   <li>异步批量写入数据库</li>
     * </ol>
     *
     * <p><strong>特点：</strong></p>
     * <ul>
     *   <li>写性能最高</li>
     *   <li>存在数据丢失风险</li>
     *   <li>适合对一致性要求不高的场景</li>
     * </ul>
     *
     * @param key 缓存 Key
     * @param value 新值
     */
    public void writeBehind(String key, Object value) {
        // 1. 更新缓存（立即返回）
        redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.debug("[Write-Behind] 更新缓存，key={}", key);

        // 2. 异步写入数据库（实际应使用消息队列或定时任务）
        // TODO: 异步批量写入数据库
        // asyncBatchWriter.addToQueue(key, value);
        
        log.warn("[Write-Behind] 异步写入数据库功能待实现，key={}", key);
    }

    // ==================== 工具方法 ====================

    /**
     * 批量删除缓存
     *
     * @param keys 缓存 Key 列表
     */
    public void batchDelete(String... keys) {
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        log.debug("[缓存操作] 批量删除缓存，数量={}", keys.length);
    }

    /**
     * 设置缓存过期时间
     *
     * @param key 缓存 Key
     * @param minutes 过期时间（分钟）
     */
    public void setExpire(String key, int minutes) {
        redisTemplate.expire(key, minutes, TimeUnit.MINUTES);
        log.debug("[缓存操作] 设置过期时间，key={}, minutes={}", key, minutes);
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存 Key
     * @return 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
