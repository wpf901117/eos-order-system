package com.eos.order.event.handlers;

import com.eos.order.event.events.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单支付事件处理器
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class OrderPaidEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("[事件处理] 订单支付成功，orderNo={}, amount={}",
                event.getOrderNo(), event.getPaidAmount());

        // 1. 发送支付成功通知
        sendPaymentSuccessNotification(event);

        // 2. 通知仓库发货
        notifyWarehouse(event);

        // 3. 更新积分
        updatePoints(event);
    }

    private void sendPaymentSuccessNotification(OrderPaidEvent event) {
        log.info("[事件处理] 发送支付成功通知，orderNo={}", event.getOrderNo());
    }

    private void notifyWarehouse(OrderPaidEvent event) {
        log.info("[事件处理] 通知仓库准备发货，orderNo={}", event.getOrderNo());
    }

    private void updatePoints(OrderPaidEvent event) {
        log.info("[事件处理] 更新用户积分，orderNo={}", event.getOrderNo());
    }
}
