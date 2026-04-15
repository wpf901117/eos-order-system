package com.eos.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单编号，唯一 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 购买数量 */
    private Integer quantity;

    /** 商品单价（快照） */
    private BigDecimal unitPrice;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 收货地址 */
    private String address;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
