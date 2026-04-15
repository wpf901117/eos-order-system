package com.eos.order.dto;

/**
 * 订单统计结果（JDK 21 Record 示例）
 *
 * <p>Record 是 JDK 16 引入的特性，在 JDK 21 中更加成熟。</p>
 *
 * <p><strong>优势：</strong></p>
 * <ul>
 *   <li>不可变数据载体</li>
 *   <li>自动生成 constructor、equals、hashCode、toString</li>
 *   <li>代码简洁，语义清晰</li>
 * </ul>
 *
 * @param userId 用户ID
 * @param totalOrders 总订单数
 * @param totalAmount 总金额
 * @param paidOrders 已支付订单数
 * @param unpaidOrders 未支付订单数
 *
 * @author EOS Team
 * @since 1.0.0
 */
public record OrderStatisticsDTO(
        Long userId,
        Integer totalOrders,
        java.math.BigDecimal totalAmount,
        Integer paidOrders,
        Integer unpaidOrders
) {
    /**
     * 紧凑构造函数：可以添加验证逻辑
     */
    public OrderStatisticsDTO {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID必须为正数");
        }
        if (totalOrders == null || totalOrders < 0) {
            throw new IllegalArgumentException("订单数不能为负数");
        }
        if (totalAmount == null || totalAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
    }

    /**
     * 工厂方法：创建空统计结果
     */
    public static OrderStatisticsDTO empty(Long userId) {
        return new OrderStatisticsDTO(userId, 0, java.math.BigDecimal.ZERO, 0, 0);
    }
}
