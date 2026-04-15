package com.eos.order.event.handlers;

import com.eos.order.event.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单创建事件处理器
 *
 * <p>监听订单创建事件，执行后续业务逻辑。</p>
 *
 * <p><strong>典型应用场景：</strong></p>
 * <ul>
 *   <li>发送通知（邮件、短信、推送）</li>
 *   <li>更新统计数据</li>
 *   <li>触发风控检查</li>
 *   <li>记录审计日志</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class OrderCreatedEventHandler {

    /**
     * 处理订单创建事件
     *
     * <p>在事务提交后异步执行，不影响主流程性能。</p>
     *
     * @param event 订单创建事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[事件处理] 订单创建成功，orderNo={}, userId={}, amount={}",
                event.getOrderNo(), event.getUserId(), event.getTotalAmount());

        // 1. 发送通知
        sendNotification(event);

        // 2. 更新用户统计
        updateUserStatistics(event);

        // 3. 触发风控检查
        triggerRiskControl(event);
    }

    /**
     * 发送通知
     */
    private void sendNotification(OrderCreatedEvent event) {
        log.info("[事件处理] 发送订单创建通知，orderNo={}", event.getOrderNo());
        // TODO: 调用消息服务发送邮件/短信
    }

    /**
     * 更新用户统计
     */
    private void updateUserStatistics(OrderCreatedEvent event) {
        log.info("[事件处理] 更新用户订单统计，userId={}", event.getUserId());
        // TODO: 更新用户订单数、消费金额等统计
    }

    /**
     * 触发风控检查
     */
    private void triggerRiskControl(OrderCreatedEvent event) {
        log.info("[事件处理] 触发风控检查，userId={}, amount={}", 
                event.getUserId(), event.getTotalAmount());
        // TODO: 调用风控服务检查异常行为
    }
}
