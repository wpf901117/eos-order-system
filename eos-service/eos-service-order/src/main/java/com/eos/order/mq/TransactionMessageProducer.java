package com.eos.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 事务消息生产者
 *
 * <p>用于保证本地事务与消息发送的最终一致性。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>订单创建成功后，通知积分服务增加积分</li>
 *   <li>支付成功后，通知物流服务发货</li>
 *   <li>库存扣减成功后，通知统计服务更新数据</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TransactionMessageProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TransactionMessageListener transactionMessageListener;

    /**
     * 事务消息 Topic
     */
    private static final String TRANSACTION_TOPIC = "eos-transaction-topic";

    /**
     * 发送事务消息
     *
     * @param tag 消息标签
     * @param payload 消息内容
     * @param businessData 业务数据（传递给本地事务）
     */
    public void sendTransactionMessage(String tag, Object payload, Object businessData) {
        String destination = TRANSACTION_TOPIC + ":" + tag;
        
        log.info("[事务消息] 开始发送事务消息，destination={}", destination);

        // 构建消息
        Message<Object> message = MessageBuilder.withPayload(payload).build();

        try {
            // 发送事务消息
            rocketMQTemplate.sendMessageInTransaction(
                destination,
                message,
                businessData  // 传递给 TransactionListener.executeLocalTransaction
            );

            log.info("[事务消息] 事务消息发送成功，destination={}", destination);

        } catch (Exception e) {
            log.error("[事务消息] 事务消息发送失败，destination={}", destination, e);
            throw new RuntimeException("事务消息发送失败", e);
        }
    }

    /**
     * 发送订单创建事务消息
     *
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param amount 金额
     */
    public void sendOrderCreatedTransaction(String orderNo, Long userId, java.math.BigDecimal amount) {
        OrderTransactionMessage message = new OrderTransactionMessage();
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setAmount(amount);

        sendTransactionMessage("order-created", message, orderNo);
    }

    /**
     * 订单事务消息 DTO
     */
    public static class OrderTransactionMessage {
        private String orderNo;
        private Long userId;
        private java.math.BigDecimal amount;

        // Getters and Setters
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    }
}
