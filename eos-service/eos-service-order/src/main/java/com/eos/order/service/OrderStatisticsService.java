package com.eos.order.service;

import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import com.eos.order.dto.OrderStatisticsDTO;
import com.eos.order.entity.Order;
import com.eos.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单统计服务（JDK 21 Pattern Matching 示例）
 *
 * <p>展示如何使用 JDK 21 的模式匹配和 Switch 表达式。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class OrderStatisticsService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 获取用户订单统计（使用 Record 和 Pattern Matching）
     *
     * @param userId 用户ID
     * @return 订单统计结果
     */
    public OrderStatisticsDTO getUserStatistics(Long userId) {
        // 查询用户所有订单
        List<Order> orders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
        );

        if (orders.isEmpty()) {
            return OrderStatisticsDTO.empty(userId);
        }

        // 使用 Stream 和 Record 计算统计信息
        int totalOrders = orders.size();
        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidOrders = orders.stream()
                .filter(order -> order.getStatus() == 1) // 已支付
                .count();

        long unpaidOrders = orders.stream()
                .filter(order -> order.getStatus() == 0) // 待支付
                .count();

        return new OrderStatisticsDTO(userId, totalOrders, totalAmount, 
                                      (int) paidOrders, (int) unpaidOrders);
    }

    /**
     * 根据订单状态获取描述（使用 Switch 表达式 - JDK 14+）
     *
     * @param status 订单状态
     * @return 状态描述
     */
    public String getStatusDescription(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case null, default -> "未知状态";
        };
    }

    /**
     * 处理订单（使用 Pattern Matching for switch - JDK 21）
     *
     * @param order 订单对象
     * @return 处理结果
     */
    public String processOrder(Object order) {
        return switch (order) {
            case Order o when o.getStatus() == 0 -> 
                "待支付订单，订单号: " + o.getOrderNo();
            case Order o when o.getStatus() == 1 -> 
                "已支付订单，金额: " + o.getTotalAmount();
            case Order o when o.getStatus() >= 2 -> 
                "已完成流程的订单";
            case Order o -> 
                "其他状态的订单";
            case null -> 
                "订单为空";
            default -> 
                "非订单对象";
        };
    }

    /**
     * 计算订单折扣（使用 Guarded Patterns - JDK 21）
     *
     * @param amount 订单金额
     * @param userType 用户类型
     * @return 折扣后金额
     */
    public BigDecimal calculateDiscount(BigDecimal amount, String userType) {
        return switch (userType) {
            case "VIP" when amount.compareTo(new BigDecimal("1000")) > 0 -> 
                amount.multiply(new BigDecimal("0.8")); // VIP且金额>1000，8折
            case "VIP" -> 
                amount.multiply(new BigDecimal("0.9")); // VIP，9折
            case "NORMAL" when amount.compareTo(new BigDecimal("500")) > 0 -> 
                amount.multiply(new BigDecimal("0.95")); // 普通用户且金额>500，95折
            case "NORMAL" -> 
                amount; // 普通用户，无折扣
            case null, default -> 
                throw new BizException(ResultCode.BAD_REQUEST, "无效的用户类型");
        };
    }
}
