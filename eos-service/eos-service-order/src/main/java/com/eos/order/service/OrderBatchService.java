package com.eos.order.service;

import com.eos.order.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 订单批量处理服务（JDK 21 虚拟线程示例）
 *
 * <p>展示如何使用 JDK 21 的虚拟线程处理高并发任务。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class OrderBatchService {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    @Autowired
    private OrderService orderService;

    /**
     * 批量查询订单详情（使用虚拟线程并行处理）
     *
     * <p>传统方式：需要管理线程池大小，担心线程耗尽</p>
     * <p>虚拟线程：可以创建成千上万个线程，无需担心资源问题</p>
     *
     * @param orderIds 订单ID列表
     * @return 订单详情列表
     */
    public List<OrderVO> batchGetOrderDetails(List<Long> orderIds) {
        log.info("[虚拟线程] 开始批量查询订单，数量={}", orderIds.size());

        // 使用虚拟线程并行处理每个订单查询
        List<OrderVO> results = orderIds.parallelStream()
                .map(orderId -> {
                    // 每个任务在一个虚拟线程中执行
                    try {
                        return orderService.getOrderById(orderId, null, "ADMIN");
                    } catch (Exception e) {
                        log.error("[虚拟线程] 查询订单失败，orderId={}", orderId, e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .collect(Collectors.toList());

        log.info("[虚拟线程] 批量查询完成，成功={}/{}", results.size(), orderIds.size());
        return results;
    }

    /**
     * 异步发送订单通知（使用虚拟线程）
     *
     * @param orderId 订单ID
     */
    public void sendOrderNotificationAsync(Long orderId) {
        virtualThreadExecutor.submit(() -> {
            try {
                log.info("[虚拟线程] 开始发送订单通知，orderId={}", orderId);
                
                // 模拟发送通知（邮件、短信、推送等）
                Thread.sleep(100); // 模拟 I/O 阻塞
                
                log.info("[虚拟线程] 订单通知发送成功，orderId={}", orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[虚拟线程] 发送通知被中断", e);
            } catch (Exception e) {
                log.error("[虚拟线程] 发送通知失败，orderId={}", orderId, e);
            }
        });
    }
}
