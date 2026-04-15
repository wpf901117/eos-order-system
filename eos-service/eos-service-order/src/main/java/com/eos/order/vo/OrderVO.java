package com.eos.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 购买数量 */
    private Integer quantity;

    /** 商品单价 */
    private BigDecimal unitPrice;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态 */
    private Integer status;

    /** 状态描述 */
    private String statusText;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 收货地址 */
    private String address;

    /** 创建时间 */
    private LocalDateTime createTime;
}
