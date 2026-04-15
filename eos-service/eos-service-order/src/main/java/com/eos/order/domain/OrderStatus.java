package com.eos.order.domain;

import lombok.Getter;

/**
 * 订单状态枚举（值对象）
 *
 * <p>使用枚举实现类型安全的状态管理。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public enum OrderStatus {

    /** 待支付 */
    PENDING_PAYMENT(0, "待支付"),

    /** 已支付 */
    PAID(1, "已支付"),

    /** 已发货 */
    SHIPPED(2, "已发货"),

    /** 已完成 */
    COMPLETED(3, "已完成"),

    /** 已取消 */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取状态
     *
     * @param code 状态代码
     * @return 订单状态
     */
    public static OrderStatus fromCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的订单状态代码: " + code);
    }
}
