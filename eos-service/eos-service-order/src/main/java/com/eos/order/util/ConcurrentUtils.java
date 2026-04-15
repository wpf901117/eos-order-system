package com.eos.order.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 并发编程工具类
 *
 * <p>提供常用的并发编程工具和最佳实践示例。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
public class ConcurrentUtils {

    /**
     * 并行执行多个任务（带超时控制）
     *
     * <p><strong>使用场景：</strong></p>
     * <ul>
     *   <li>并行查询多个数据源</li>
     *   <li>并行调用多个外部接口</li>
     *   <li>批量数据处理</li>
     * </ul>
     *
     * @param tasks 任务列表
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param executor 线程池
     * @return 任务结果列表
     */
    public static <T> List<T> parallelExecute(
            List<Callable<T>> tasks,
            long timeout,
            TimeUnit unit,
            ExecutorService executor) throws Exception {

        // 提交所有任务
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(executor.submit(task));
        }

        // 收集结果
        List<T> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                T result = futures.get(i).get(timeout, unit);
                results.add(result);
            } catch (TimeoutException e) {
                log.error("[并发工具] 任务 {} 执行超时", i, e);
                futures.get(i).cancel(true);
            } catch (ExecutionException e) {
                log.error("[并发工具] 任务 {} 执行异常", i, e);
            }
        }

        return results;
    }

    /**
     * 并行处理集合数据
     *
     * @param dataList 数据列表
     * @param processor 处理函数
     * @param threadPoolSize 线程池大小
     * @return 处理结果列表
     */
    public static <T, R> List<R> parallelProcess(
            List<T> dataList,
            Function<T, R> processor,
            int threadPoolSize) {

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try {
            // 提交任务
            List<Future<R>> futures = dataList.stream()
                .map(data -> executor.submit(() -> processor.apply(data)))
                .collect(Collectors.toList());

            // 收集结果
            return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("[并发工具] 并行处理异常", e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .collect(Collectors.toList());

        } finally {
            executor.shutdown();
        }
    }

    /**
     * 使用 CompletableFuture 并行执行
     *
     * @param tasks 任务列表
     * @param executor 线程池
     * @return 结果列表
     */
    public static <T> List<T> parallelWithCompletableFuture(
            List<Supplier<T>> tasks,
            Executor executor) {

        List<CompletableFuture<T>> futures = tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(task, executor))
            .collect(Collectors.toList());

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    /**
     * 限流执行器（令牌桶算法简化版）
     */
    public static class RateLimiter {
        private final Semaphore semaphore;

        public RateLimiter(int permits) {
            this.semaphore = new Semaphore(permits);
        }

        /**
         * 执行任务（受限于速率）
         *
         * @param task 任务
         */
        public void execute(Runnable task) {
            try {
                semaphore.acquire();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[限流执行器] 任务被中断", e);
            } finally {
                semaphore.release();
            }
        }
    }

    /**
     * 读写锁封装
     */
    public static class ReadWriteLockHelper {
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        /**
         * 读操作
         *
         * @param action 读操作
         * @return 读取结果
         */
        public <T> T read(Supplier<T> action) {
            readWriteLock.readLock().lock();
            try {
                return action.get();
            } finally {
                readWriteLock.readLock().unlock();
            }
        }

        /**
         * 写操作
         *
         * @param action 写操作
         */
        public void write(Runnable action) {
            readWriteLock.writeLock().lock();
            try {
                action.run();
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    /**
     * 原子计数器
     */
    public static class AtomicCounter {
        private final LongAdder counter = new LongAdder();

        public void increment() {
            counter.increment();
        }

        public void decrement() {
            counter.decrement();
        }

        public long get() {
            return counter.sum();
        }

        public void reset() {
            counter.reset();
        }
    }
}
