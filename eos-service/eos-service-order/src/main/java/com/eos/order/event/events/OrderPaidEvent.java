package com.eos.order.event.events;

import com.eos.order.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付事件
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class OrderPaidEvent extends DomainEvent {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private final String orderNo;

    /** 支付金额 */
    private final BigDecimal paidAmount;

    /** 支付时间 */
    private final LocalDateTime paidTime;

    public OrderPaidEvent(Long orderId, String orderNo, BigDecimal paidAmount, LocalDateTime paidTime) {
        super(orderId);
        this.orderNo = orderNo;
        this.paidAmount = paidAmount;
        this.paidTime = paidTime;
    }
}
