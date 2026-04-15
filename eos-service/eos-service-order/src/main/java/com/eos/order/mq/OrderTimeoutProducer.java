package com.eos.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息生产者
 *
 * <p>RocketMQ 延迟消息是实现订单超时自动取消的经典方案。
 * 相比定时轮询数据库，延迟消息具有以下优势：</p>
 * <ul>
 *   <li>精确触发：订单到达设定时间后立刻被处理</li>
 *   <li>降低数据库压力：无需定时扫描全表</li>
 *   <li>削峰填谷：消费者按需处理，避免集中爆发</li>
 * </ul>
 *
 * <p><strong>RocketMQ 延迟级别：</strong></p>
 * <pre>
 * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
 * 级别1   2   3    4   5  6  7  8  9  10 11 12 13 14  15  16  17 18
 * </pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class OrderTimeoutProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /** 订单超时Topic */
    private static final String TOPIC_ORDER_TIMEOUT = "order-timeout-topic";

    /**
     * 发送订单超时延迟消息
     *
     * @param orderId   订单ID
     * @param delayMin  延迟分钟数（实际发送时会映射到最接近的RocketMQ延迟级别）
     */
    public void sendDelayMessage(Long orderId, int delayMin) {
        // RocketMQ 延迟级别映射
        int delayLevel = mapDelayLevel(delayMin);

        try {
            org.springframework.messaging.Message<Long> message = MessageBuilder
                    .withPayload(orderId)
                    .setHeader("DELAY", delayLevel)
                    .build();
            rocketMQTemplate.syncSendDelayTimeSeconds(
                    TOPIC_ORDER_TIMEOUT,
                    message,
                    3000  // 3秒超时
            );
            log.info("[MQ发送] 订单超时延迟消息已发送，orderId={}，delayLevel={}，delayMin={}",
                    orderId, delayLevel, delayMin);
        } catch (Exception e) {
            log.error("[MQ发送] 订单超时延迟消息发送失败，orderId={}", orderId, e);
        }
    }

    /**
     * 将分钟数映射到RocketMQ延迟级别
     */
    private int mapDelayLevel(int delayMin) {
        if (delayMin <= 1) return 5;   // 1m
        if (delayMin <= 2) return 6;   // 2m
        if (delayMin <= 3) return 7;   // 3m
        if (delayMin <= 5) return 9;   // 5m
        if (delayMin <= 10) return 14; // 10m
        if (delayMin <= 20) return 15; // 20m
        if (delayMin <= 30) return 16; // 30m
        if (delayMin <= 60) return 17; // 1h
        return 18; // 2h
    }
}
