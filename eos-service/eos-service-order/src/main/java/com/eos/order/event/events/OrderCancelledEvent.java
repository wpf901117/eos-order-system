package com.eos.order.event.events;

import com.eos.order.event.DomainEvent;
import lombok.Getter;

/**
 * 订单取消事件
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class OrderCancelledEvent extends DomainEvent {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private final String orderNo;

    /** 取消原因 */
    private final String reason;

    public OrderCancelledEvent(Long orderId, String orderNo, String reason) {
        super(orderId);
        this.orderNo = orderNo;
        this.reason = reason;
    }
}
