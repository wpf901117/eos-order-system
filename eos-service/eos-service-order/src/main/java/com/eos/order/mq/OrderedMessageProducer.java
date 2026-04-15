package com.eos.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 顺序消息生产者
 *
 * <p>保证同一业务ID的消息按顺序消费。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>订单状态变更：创建 → 支付 → 发货 → 完成（必须按顺序）</li>
 *   <li>账户流水：充值、消费、退款（必须按时间顺序）</li>
 * </ul>
 *
 * <p><strong>实现原理：</strong></p>
 * <ul>
 *   <li>相同 hashKey 的消息路由到同一队列</li>
 *   <li>消费者单线程消费同一队列</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class OrderedMessageProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 顺序消息 Topic
     */
    private static final String ORDERED_TOPIC = "eos-ordered-topic";

    /**
     * 发送顺序消息
     *
     * @param tag 消息标签
     * @param payload 消息内容
     * @param hashKey 哈希键（相同 key 的消息保证顺序）
     */
    public void sendOrderedMessage(String tag, Object payload, String hashKey) {
        String destination = ORDERED_TOPIC + ":" + tag;
        
        log.info("[顺序消息] 发送顺序消息，destination={}, hashKey={}", destination, hashKey);

        // 构建消息
        Message<Object> message = MessageBuilder.withPayload(payload).build();

        try {
            // 发送顺序消息（syncSendOrderly 保证顺序）
            rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
            
            log.info("[顺序消息] 顺序消息发送成功，destination={}, hashKey={}", destination, hashKey);

        } catch (Exception e) {
            log.error("[顺序消息] 顺序消息发送失败，destination={}, hashKey={}", destination, hashKey, e);
            throw new RuntimeException("顺序消息发送失败", e);
        }
    }

    /**
     * 发送订单状态变更顺序消息
     *
     * @param orderNo 订单号
     * @param status 新状态
     * @param oldStatus 旧状态
     */
    public void sendOrderStatusChangeMessage(String orderNo, Integer status, Integer oldStatus) {
        OrderStatusMessage message = new OrderStatusMessage();
        message.setOrderNo(orderNo);
        message.setStatus(status);
        message.setOldStatus(oldStatus);
        message.setTimestamp(System.currentTimeMillis());

        // 使用订单号作为 hashKey，保证同一订单的状态变更按顺序
        sendOrderedMessage("order-status-change", message, orderNo);
    }

    /**
     * 订单状态消息 DTO
     */
    public static class OrderStatusMessage {
        private String orderNo;
        private Integer status;
        private Integer oldStatus;
        private Long timestamp;

        // Getters and Setters
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getOldStatus() { return oldStatus; }
        public void setOldStatus(Integer oldStatus) { this.oldStatus = oldStatus; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
