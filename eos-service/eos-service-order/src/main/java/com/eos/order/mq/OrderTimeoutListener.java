package com.eos.order.mq;

import com.eos.order.entity.Order;
import com.eos.order.mapper.OrderMapper;
import com.eos.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息消费者
 *
 * <p>监听订单超时Topic，在订单超时后执行自动取消逻辑。
 * 这是延迟消息的经典应用场景。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-timeout-topic",
        consumerGroup = "order-timeout-consumer-group",
        selectorExpression = "*"
)
public class OrderTimeoutListener implements RocketMQListener<Long> {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    /**
     * 处理订单超时消息
     *
     * <p>消费流程：</p>
     * <ol>
     *   <li>根据订单ID查询订单</li>
     *   <li>如果订单仍是"待支付"状态，则自动取消</li>
     *   <li>如果订单已支付或其他状态，则忽略</li>
     * </ol>
     *
     * @param orderId 订单ID
     */
    @Override
    public void onMessage(Long orderId) {
        log.info("[MQ消费] 收到订单超时消息，orderId={}", orderId);

        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.warn("[MQ消费] 订单不存在，orderId={}", orderId);
                return;
            }

            if (order.getStatus() != 0) {
                log.info("[MQ消费] 订单状态不是待支付，无需取消，orderId={}，status={}",
                        orderId, order.getStatus());
                return;
            }

            // 执行超时取消（包含库存回滚）
            orderService.timeoutCancelOrder(orderId);
            log.info("[MQ消费] 订单已自动取消，orderId={}", orderId);

        } catch (Exception e) {
            log.error("[MQ消费] 订单超时处理异常，orderId={}", orderId, e);
            // 抛出异常让RocketMQ重试
            throw new RuntimeException("订单超时处理失败", e);
        }
    }
}
