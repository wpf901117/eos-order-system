package com.eos.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 虚拟线程配置
 *
 * <p>JDK 21 引入了虚拟线程（Virtual Threads），也称为轻量级线程。</p>
 *
 * <p><strong>优势：</strong></p>
 * <ul>
 *   <li>极低的内存占用（每个虚拟线程约几KB，平台线程约1MB）</li>
 *   <li>可以创建数百万个虚拟线程</li>
 *   <li>简化并发编程，无需复杂的线程池管理</li>
 *   <li>适合 I/O 密集型任务</li>
 * </ul>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>高并发 HTTP 请求处理</li>
 *   <li>批量数据处理</li>
 *   <li>异步任务执行</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class VirtualThreadConfig {

    /**
     * 创建虚拟线程执行器
     *
     * <p>适用于 I/O 密集型任务，如：
     * - 远程服务调用
     * - 数据库查询
     * - 文件读写
     */
    @Bean("virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        log.info("[虚拟线程] 创建虚拟线程执行器");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建平台线程执行器（用于 CPU 密集型任务）
     *
     * <p>CPU 密集型任务不适合使用虚拟线程，因为：
     * - 虚拟线程在阻塞时才会让出 CPU
     * - CPU 密集型任务会一直占用 CPU，无法发挥虚拟线程优势
     */
    @Bean("platformThreadExecutor")
    public ExecutorService platformThreadExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        log.info("[平台线程] 创建平台线程执行器，核心数={}", cores);
        return Executors.newFixedThreadPool(cores);
    }
}
