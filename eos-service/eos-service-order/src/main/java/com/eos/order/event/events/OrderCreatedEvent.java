package com.eos.order.event.events;

import com.eos.order.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 订单创建事件
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private final String orderNo;

    /** 用户ID */
    private final Long userId;

    /** 商品ID */
    private final Long productId;

    /** 商品名称 */
    private final String productName;

    /** 数量 */
    private final Integer quantity;

    /** 总金额 */
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Long orderId, String orderNo, Long userId, 
                             Long productId, String productName, 
                             Integer quantity, BigDecimal totalAmount) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }
}
