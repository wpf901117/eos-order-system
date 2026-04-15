package com.eos.order.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建消息
 *
 * <p>用于分布式事务中的最终一致性方案，通过 RocketMQ 事务消息实现。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String address;
}
