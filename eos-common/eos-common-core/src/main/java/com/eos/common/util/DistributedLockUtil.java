package com.eos.common.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 *
 * <p>基于 Redisson 实现分布式锁，提供简单易用的 API。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>防止超卖（库存扣减）</li>
 *   <li>防止重复提交（订单创建）</li>
 *   <li>定时任务防重执行</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 方式1：手动加锁
 * RLock lock = DistributedLockUtil.lock(redissonClient, "order:create:" + userId);
 * try {
 *     // 业务逻辑
 * } finally {
 *     DistributedLockUtil.unlock(lock);
 * }
 *
 * // 方式2：函数式编程（推荐）
 * DistributedLockUtil.executeWithLock(redissonClient, "order:create:" + userId, () -> {
 *     // 业务逻辑
 *     return result;
 * });
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
public class DistributedLockUtil {

    /**
     * 尝试获取分布式锁
     *
     * @param redissonClient Redisson 客户端
     * @param lockKey        锁的键
     * @return RLock 对象
     */
    public static RLock lock(RedissonClient redissonClient, String lockKey) {
        return lock(redissonClient, lockKey, 3, 10, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取分布式锁（自定义超时时间）
     *
     * @param redissonClient Redisson 客户端
     * @param lockKey        锁的键
     * @param waitTime       等待时间（秒）
     * @param leaseTime      锁持有时间（秒）
     * @return RLock 对象
     */
    public static RLock lock(RedissonClient redissonClient, String lockKey, 
                             long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, unit);
            if (!locked) {
                log.warn("[分布式锁] 获取锁失败，lockKey={}", lockKey);
                return null;
            }
            log.debug("[分布式锁] 获取锁成功，lockKey={}", lockKey);
            return lock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取锁被中断，lockKey={}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁
     *
     * @param lock RLock 对象
     */
    public static void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("[分布式锁] 释放锁成功");
            } catch (IllegalMonitorStateException e) {
                log.warn("[分布式锁] 释放锁异常", e);
            }
        }
    }

    /**
     * 在分布式锁保护下执行业务逻辑（无返回值）
     *
     * @param redissonClient Redisson 客户端
     * @param lockKey        锁的键
     * @param runnable       业务逻辑
     */
    public static void executeWithLock(RedissonClient redissonClient, String lockKey, Runnable runnable) {
        executeWithLock(redissonClient, lockKey, 3, 10, TimeUnit.SECONDS, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 在分布式锁保护下执行业务逻辑（有返回值）
     *
     * @param redissonClient Redisson 客户端
     * @param lockKey        锁的键
     * @param supplier       业务逻辑
     * @param <T>            返回值类型
     * @return 业务逻辑返回值
     */
    public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey, Supplier<T> supplier) {
        return executeWithLock(redissonClient, lockKey, 3, 10, TimeUnit.SECONDS, supplier);
    }

    /**
     * 在分布式锁保护下执行业务逻辑（自定义超时时间）
     *
     * @param redissonClient Redisson 客户端
     * @param lockKey        锁的键
     * @param waitTime       等待时间
     * @param leaseTime      锁持有时间
     * @param unit           时间单位
     * @param supplier       业务逻辑
     * @param <T>            返回值类型
     * @return 业务逻辑返回值
     */
    public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey,
                                        long waitTime, long leaseTime, TimeUnit unit,
                                        Supplier<T> supplier) {
        RLock lock = null;
        try {
            lock = lock(redissonClient, lockKey, waitTime, leaseTime, unit);
            if (lock == null) {
                throw new RuntimeException("获取分布式锁失败: " + lockKey);
            }
            return supplier.get();
        } finally {
            unlock(lock);
        }
    }
}
