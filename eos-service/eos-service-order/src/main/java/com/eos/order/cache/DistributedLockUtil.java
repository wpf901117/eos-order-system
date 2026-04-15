package com.eos.order.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁高级工具
 *
 * <p>提供多种分布式锁实现：</p>
 * <ul>
 *   <li>可重入锁（Reentrant Lock）</li>
 *   <li>公平锁（Fair Lock）</li>
 *   <li>读写锁（ReadWrite Lock）</li>
 *   <li>红锁（RedLock）- 多节点高可用</li>
 *   <li>联锁（MultiLock）</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class DistributedLockUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 使用可重入锁执行任务
     *
     * @param lockKey 锁的 Key
     * @param leaseTime 锁自动释放时间（秒）
     * @param task 要执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            // 尝试加锁，最多等待 10 秒，锁定后 30 秒自动释放
            locked = lock.tryLock(10, leaseTime, TimeUnit.SECONDS);
            
            if (!locked) {
                log.warn("[分布式锁] 获取锁失败，lockKey={}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.debug("[分布式锁] 获取锁成功，lockKey={}", lockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取锁被中断，lockKey={}", lockKey, e);
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            // 只有当前线程持有锁时才释放
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[分布式锁] 释放锁成功，lockKey={}", lockKey);
            }
        }
    }

    /**
     * 使用公平锁执行任务
     *
     * <p>公平锁按照请求顺序获取锁，避免饥饿问题。</p>
     *
     * @param lockKey 锁的 Key
     * @param task 要执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithFairLock(String lockKey, Supplier<T> task) {
        RLock lock = redissonClient.getFairLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 使用读写锁执行读任务
     *
     * <p>读锁可以并发，写锁互斥。</p>
     *
     * @param lockKey 锁的 Key
     * @param task 读任务
     * @return 任务执行结果
     */
    public <T> T executeWithReadLock(String lockKey, Supplier<T> task) {
        RLock readLock = redissonClient.getReadWriteLock(lockKey).readLock();
        boolean locked = false;

        try {
            locked = readLock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            if (locked && readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 使用读写锁执行写任务
     *
     * @param lockKey 锁的 Key
     * @param task 写任务
     * @return 任务执行结果
     */
    public <T> T executeWithWriteLock(String lockKey, Supplier<T> task) {
        RLock writeLock = redissonClient.getReadWriteLock(lockKey).writeLock();
        boolean locked = false;

        try {
            locked = writeLock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            if (locked && writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    /**
     * RedLock - 多节点高可用分布式锁
     *
     * <p>适用于对可用性要求极高的场景。</p>
     *
     * @param lockKeys 多个锁的 Key（不同 Redis 节点）
     * @param task 要执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithRedLock(String[] lockKeys, Supplier<T> task) {
        // TODO: 实际生产环境需要配置多个 RedissonClient 实例
        // RedissonClient client1 = Redisson.create(config1);
        // RedissonClient client2 = Redisson.create(config2);
        // RedissonClient client3 = Redisson.create(config3);
        
        // RLock lock1 = client1.getLock(lockKeys[0]);
        // RLock lock2 = client2.getLock(lockKeys[1]);
        // RLock lock3 = client3.getLock(lockKeys[2]);
        
        // RLock redLock = redissonClient.getRedLock(lock1, lock2, lock3);
        
        // 这里简化为单节点示例
        RLock lock = redissonClient.getLock(lockKeys[0]);
        boolean locked = false;

        try {
            locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 联锁 - 同时获取多个锁
     *
     * @param lockKeys 多个锁的 Key
     * @param task 要执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithMultiLock(String[] lockKeys, Supplier<T> task) {
        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }

        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean locked = false;

        try {
            locked = multiLock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");

        } finally {
            if (locked && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
}
