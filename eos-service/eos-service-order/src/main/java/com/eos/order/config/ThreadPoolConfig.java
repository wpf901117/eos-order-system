package com.eos.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * <p>统一管理应用中的线程池，避免资源浪费和线程泄漏。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    /**
     * 业务异步线程池
     *
     * <p>用于处理耗时的业务逻辑，如发送通知、更新统计等。</p>
     */
    @Bean("businessExecutor")
    public Executor businessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：CPU 核心数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数：CPU 核心数 * 2
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量：1000
        executor.setQueueCapacity(1000);
        
        // 线程名前缀
        executor.setThreadNamePrefix("business-async-");
        
        // 拒绝策略：由调用线程处理（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("[线程池] 业务异步线程池初始化完成，核心线程: {}, 最大线程: {}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * IO 密集型线程池
     *
     * <p>用于处理 IO 密集型任务，如文件上传、HTTP 请求等。</p>
     */
    @Bean("ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // IO 密集型：核心线程数 = CPU 核心数 * 2
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数：CPU 核心数 * 4
        executor.setMaxPoolSize(corePoolSize * 2);
        
        // 队列容量：2000
        executor.setQueueCapacity(2000);
        
        // 线程名前缀
        executor.setThreadNamePrefix("io-async-");
        
        // 拒绝策略：丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.setKeepAliveSeconds(120);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("[线程池] IO 密集型线程池初始化完成，核心线程: {}, 最大线程: {}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * 定时任务线程池
     *
     * <p>用于执行定时任务，如对账、数据清理等。</p>
     */
    @Bean("scheduledExecutor")
    public Executor scheduledExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 定时任务不需要太多线程
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        
        executor.setThreadNamePrefix("scheduled-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("[线程池] 定时任务线程池初始化完成");
        
        return executor;
    }
}
