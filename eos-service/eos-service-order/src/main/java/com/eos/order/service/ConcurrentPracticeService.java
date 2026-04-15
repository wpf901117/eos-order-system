package com.eos.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 并发编程实战服务
 *
 * <p>展示各种并发编程场景的最佳实践。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class ConcurrentPracticeService {

    @Autowired
    private Executor businessExecutor;

    /**
     * 场景1：并行查询多个数据源并合并结果
     *
     * <p><strong>应用场景：</strong></p>
     * <ul>
     *   <li>订单详情页需要查询订单、用户、商品、物流等多个数据源</li>
     *   <li>并行查询可以显著降低响应时间</li>
     * </ul>
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    public OrderDetailVO getOrderDetailWithParallelQuery(Long orderId) {
        long startTime = System.currentTimeMillis();

        // 并行查询多个数据源
        CompletableFuture<OrderVO> orderFuture = CompletableFuture.supplyAsync(
            () -> queryOrder(orderId), businessExecutor);

        CompletableFuture<UserVO> userFuture = CompletableFuture.supplyAsync(
            () -> queryUser(orderId), businessExecutor);

        CompletableFuture<ProductVO> productFuture = CompletableFuture.supplyAsync(
            () -> queryProduct(orderId), businessExecutor);

        CompletableFuture<LogisticsVO> logisticsFuture = CompletableFuture.supplyAsync(
            () -> queryLogistics(orderId), businessExecutor);

        // 等待所有查询完成并合并结果
        CompletableFuture.allOf(orderFuture, userFuture, productFuture, logisticsFuture).join();

        OrderDetailVO detail = new OrderDetailVO();
        detail.setOrder(orderFuture.join());
        detail.setUser(userFuture.join());
        detail.setProduct(productFuture.join());
        detail.setLogistics(logisticsFuture.join());

        long duration = System.currentTimeMillis() - startTime;
        log.info("[并发实战] 并行查询订单详情完成，耗时: {}ms", duration);

        return detail;
    }

    /**
     * 场景2：批量数据处理（分片并行）
     *
     * <p><strong>应用场景：</strong></p>
     * <ul>
     *   <li>批量更新订单状态</li>
     *   <li>批量发送通知</li>
     *   <li>大数据量处理</li>
     * </ul>
     *
     * @param orderIds 订单ID列表
     */
    public void batchProcessOrders(List<Long> orderIds) {
        int batchSize = 100;  // 每批处理100条
        List<List<Long>> batches = partitionList(orderIds, batchSize);

        log.info("[并发实战] 开始批量处理订单，总数: {}, 批次数: {}", 
                 orderIds.size(), batches.size());

        // 并行处理每个批次
        List<CompletableFuture<Void>> futures = batches.stream()
            .map(batch -> CompletableFuture.runAsync(
                () -> processBatch(batch), businessExecutor))
            .collect(Collectors.toList());

        // 等待所有批次完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("[并发实战] 批量处理订单完成");
    }

    /**
     * 场景3：异步任务编排（依赖关系）
     *
     * <p><strong>应用场景：</strong></p>
     * <ul>
     *   <li>任务B依赖任务A的结果</li>
     *   <li>任务C和D并行执行，都依赖任务B</li>
     *   <li>最后汇总所有结果</li>
     * </ul>
     */
    public String asyncTaskOrchestration() {
        long startTime = System.currentTimeMillis();

        // 任务A：获取基础数据
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(
            () -> {
                log.info("[并发实战] 执行任务A");
                sleep(1000);  // 模拟耗时操作
                return "Result-A";
            }, businessExecutor);

        // 任务B：依赖任务A的结果
        CompletableFuture<String> taskB = taskA.thenApplyAsync(
            resultA -> {
                log.info("[并发实战] 执行任务B，依赖: {}", resultA);
                sleep(1000);
                return "Result-B-based-on-" + resultA;
            }, businessExecutor);

        // 任务C和D：并行执行，都依赖任务B
        CompletableFuture<String> taskC = taskB.thenApplyAsync(
            resultB -> {
                log.info("[并发实战] 执行任务C，依赖: {}", resultB);
                sleep(500);
                return "Result-C";
            }, businessExecutor);

        CompletableFuture<String> taskD = taskB.thenApplyAsync(
            resultB -> {
                log.info("[并发实战] 执行任务D，依赖: {}", resultB);
                sleep(500);
                return "Result-D";
            }, businessExecutor);

        // 汇总结果
        CompletableFuture<String> finalResult = taskC.thenCombine(taskD,
            (resultC, resultD) -> {
                log.info("[并发实战] 汇总结果");
                return resultC + " + " + resultD;
            });

        String result = finalResult.join();
        long duration = System.currentTimeMillis() - startTime;

        log.info("[并发实战] 任务编排完成，结果: {}, 耗时: {}ms", result, duration);
        return result;
    }

    /**
     * 场景4：超时控制和异常处理
     */
    public String executeWithTimeout() {
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> {
                    sleep(5000);  // 模拟耗时操作
                    return "Success";
                }, businessExecutor);

            // 设置超时时间为2秒
            return future.get(2, java.util.concurrent.TimeUnit.SECONDS);

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("[并发实战] 任务执行超时", e);
            return "Timeout";
        } catch (Exception e) {
            log.error("[并发实战] 任务执行异常", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 场景5：使用 @Async 注解实现异步方法
     *
     * @param message 消息内容
     */
    @Async("businessExecutor")
    public void sendNotificationAsync(String message) {
        log.info("[并发实战] 异步发送通知: {}", message);
        sleep(1000);  // 模拟发送通知
        log.info("[并发实战] 通知发送完成: {}", message);
    }

    // ==================== 辅助方法 ====================

    private OrderVO queryOrder(Long orderId) {
        log.info("[并发实战] 查询订单: {}", orderId);
        sleep(500);
        return new OrderVO();
    }

    private UserVO queryUser(Long orderId) {
        log.info("[并发实战] 查询用户: {}", orderId);
        sleep(300);
        return new UserVO();
    }

    private ProductVO queryProduct(Long orderId) {
        log.info("[并发实战] 查询商品: {}", orderId);
        sleep(400);
        return new ProductVO();
    }

    private LogisticsVO queryLogistics(Long orderId) {
        log.info("[并发实战] 查询物流: {}", orderId);
        sleep(600);
        return new LogisticsVO();
    }

    private void processBatch(List<Long> orderIds) {
        log.info("[并发实战] 处理批次，数量: {}", orderIds.size());
        sleep(500);
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== VO 类（简化版）====================

    public static class OrderDetailVO {
        private OrderVO order;
        private UserVO user;
        private ProductVO product;
        private LogisticsVO logistics;

        // Getters and Setters
        public OrderVO getOrder() { return order; }
        public void setOrder(OrderVO order) { this.order = order; }
        public UserVO getUser() { return user; }
        public void setUser(UserVO user) { this.user = user; }
        public ProductVO getProduct() { return product; }
        public void setProduct(ProductVO product) { this.product = product; }
        public LogisticsVO getLogistics() { return logistics; }
        public void setLogistics(LogisticsVO logistics) { this.logistics = logistics; }
    }

    public static class OrderVO {}
    public static class UserVO {}
    public static class ProductVO {}
    public static class LogisticsVO {}
}
