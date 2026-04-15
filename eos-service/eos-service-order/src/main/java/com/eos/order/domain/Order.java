package com.eos.order.domain;

import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单聚合根（DDD 充血模型）
 *
 * <p>与传统贫血模型不同，充血模型将业务逻辑封装在领域对象内部。</p>
 *
 * <p><strong>核心概念：</strong></p>
 * <ul>
 *   <li><strong>聚合根</strong>：Order 是订单聚合的根，保证聚合内的一致性</li>
 *   <li><strong>值对象</strong>：Money、Address 等不可变对象</li>
 *   <li><strong>领域事件</strong>：OrderCreatedEvent、OrderPaidEvent 等</li>
 *   <li><strong>不变性约束</strong>：通过业务方法保证状态转换的合法性</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class Order {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Money unitPrice;
    private Money totalAmount;
    private OrderStatus status;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;

    /**
     * 私有构造函数，强制使用工厂方法创建
     */
    private Order() {
    }

    /**
     * 工厂方法：创建新订单
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param productName 商品名称
     * @param quantity 数量
     * @param unitPrice 单价
     * @param address 收货地址
     * @return 新创建的订单
     */
    public static Order create(Long userId, Long productId, String productName,
                                Integer quantity, Money unitPrice, String address) {
        // 业务规则校验
        if (quantity <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "购买数量必须大于0");
        }
        if (unitPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "商品价格必须大于0");
        }

        Order order = new Order();
        order.userId = userId;
        order.productId = productId;
        order.productName = productName;
        order.quantity = quantity;
        order.unitPrice = unitPrice;
        order.totalAmount = unitPrice.multiply(quantity);
        order.status = OrderStatus.PENDING_PAYMENT;
        order.address = address;
        order.createTime = LocalDateTime.now();

        // 发布领域事件：OrderCreatedEvent
        // DomainEventPublisher.publish(new OrderCreatedEvent(order));

        return order;
    }

    /**
     * 领域行为：支付订单
     *
     * <p>状态转换：待支付 -> 已支付</p>
     */
    public void pay() {
        // 业务规则：只有待支付状态才能支付
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, 
                "订单状态不允许支付，当前状态：" + this.status.getDescription());
        }

        this.status = OrderStatus.PAID;
        this.payTime = LocalDateTime.now();

        // 发布领域事件：OrderPaidEvent
        // DomainEventPublisher.publish(new OrderPaidEvent(this.id, this.orderNo));
    }

    /**
     * 领域行为：取消订单
     *
     * <p>状态转换：待支付 -> 已取消</p>
     */
    public void cancel() {
        // 业务规则：只有待支付状态才能取消
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR,
                "订单状态不允许取消，当前状态：" + this.status.getDescription());
        }

        this.status = OrderStatus.CANCELLED;
        this.cancelTime = LocalDateTime.now();

        // 发布领域事件：OrderCancelledEvent
        // DomainEventPublisher.publish(new OrderCancelledEvent(this.id, this.orderNo));
    }

    /**
     * 领域行为：发货
     *
     * <p>状态转换：已支付 -> 已发货</p>
     */
    public void ship() {
        // 业务规则：只有已支付状态才能发货
        if (this.status != OrderStatus.PAID) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR,
                "订单状态不允许发货，当前状态：" + this.status.getDescription());
        }

        this.status = OrderStatus.SHIPPED;
        this.shipTime = LocalDateTime.now();

        // 发布领域事件：OrderShippedEvent
        // DomainEventPublisher.publish(new OrderShippedEvent(this.id, this.orderNo));
    }

    /**
     * 领域行为：确认收货
     *
     * <p>状态转换：已发货 -> 已完成</p>
     */
    public void confirm() {
        // 业务规则：只有已发货状态才能确认收货
        if (this.status != OrderStatus.SHIPPED) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR,
                "订单状态不允许确认收货，当前状态：" + this.status.getDescription());
        }

        this.status = OrderStatus.COMPLETED;
        this.completeTime = LocalDateTime.now();

        // 发布领域事件：OrderCompletedEvent
        // DomainEventPublisher.publish(new OrderCompletedEvent(this.id, this.orderNo));
    }

    /**
     * 业务方法：判断订单是否可以取消
     *
     * @return true 如果可以取消
     */
    public boolean canCancel() {
        return this.status == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 业务方法：判断订单是否超时（30分钟未支付）
     *
     * @return true 如果已超时
     */
    public boolean isTimeout() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        
        LocalDateTime timeoutThreshold = this.createTime.plusMinutes(30);
        return LocalDateTime.now().isAfter(timeoutThreshold);
    }

    /**
     * 业务方法：计算折扣后金额
     *
     * @param discountRate 折扣率（0.8 = 8折）
     * @return 折扣后金额
     */
    public Money calculateDiscountedAmount(BigDecimal discountRate) {
        if (discountRate.compareTo(BigDecimal.ZERO) <= 0 || 
            discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("折扣率必须在 0-1 之间");
        }
        
        return this.totalAmount.multiply(discountRate);
    }
}
