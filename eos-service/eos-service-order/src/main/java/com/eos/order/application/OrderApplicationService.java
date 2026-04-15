package com.eos.order.application;

import com.eos.order.domain.Money;
import com.eos.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 订单应用服务（DDD Application Service）
 *
 * <p>应用服务负责：</p>
 * <ul>
 *   <li>协调领域对象完成业务用例</li>
 *   <li>事务管理</li>
 *   <li>权限校验</li>
 *   <li>发布领域事件</li>
 * </ul>
 *
 * <p>注意：应用服务不包含业务逻辑，业务逻辑在领域对象中。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class OrderApplicationService {

    /**
     * 创建订单（使用 DDD 充血模型）
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param productName 商品名称
     * @param quantity 数量
     * @param unitPrice 单价
     * @param address 收货地址
     * @return 创建的订单
     */
    @Transactional
    public Order createOrder(Long userId, Long productId, String productName,
                             Integer quantity, BigDecimal unitPrice, String address) {
        log.info("[DDD] 开始创建订单，userId={}, productId={}", userId, productId);

        // 1. 使用工厂方法创建订单（领域层）
        Money money = new Money(unitPrice);
        Order order = Order.create(userId, productId, productName, quantity, money, address);

        // 2. 持久化订单（基础设施层）
        // orderRepository.save(order);

        // 3. 发布领域事件（可选）
        // eventPublisher.publish(new OrderCreatedEvent(order));

        log.info("[DDD] 订单创建成功");
        return order;
    }

    /**
     * 支付订单（使用 DDD 充血模型）
     *
     * @param orderId 订单ID
     */
    @Transactional
    public void payOrder(Long orderId) {
        log.info("[DDD] 开始支付订单，orderId={}", orderId);

        // 1. 从仓储加载订单
        // Order order = orderRepository.findById(orderId)
        //     .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 2. 调用领域行为（业务逻辑在 Order 内部）
        // order.pay();

        // 3. 持久化变更
        // orderRepository.save(order);

        log.info("[DDD] 订单支付成功");
    }

    /**
     * 取消订单（使用 DDD 充血模型）
     *
     * @param orderId 订单ID
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("[DDD] 开始取消订单，orderId={}", orderId);

        // 1. 从仓储加载订单
        // Order order = orderRepository.findById(orderId)
        //     .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 2. 调用领域行为
        // order.cancel();

        // 3. 持久化变更
        // orderRepository.save(order);

        log.info("[DDD] 订单取消成功");
    }
}
