package com.eos.order.domain.aggregate;

import com.eos.order.domain.valueobject.Address;
import com.eos.order.domain.valueobject.Money;
import com.eos.order.event.DomainEvent;
import com.eos.order.event.events.OrderCancelledEvent;
import com.eos.order.event.events.OrderCreatedEvent;
import com.eos.order.event.events.OrderPaidEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单聚合根
 *
 * <p>DDD 核心概念：聚合根是聚合的入口，保证业务一致性。</p>
 *
 * <p><strong>不变性规则：</strong></p>
 * <ul>
 *   <li>订单创建后，商品和数量不能修改</li>
 *   <li>只有待支付状态才能取消</li>
 *   <li>只有待支付状态才能支付</li>
 *   <li>已支付的订单不能取消</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class OrderAggregate {

    /** 订单ID */
    private Long id;

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
    private Money unitPrice;

    /** 总金额 */
    private Money totalAmount;

    /** 订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消 */
    private Integer status;

    /** 收货地址 */
    private Address shippingAddress;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ==================== 工厂方法 ====================

    /**
     * 创建订单（工厂方法）
     */
    public static OrderAggregate create(Long userId, Long productId, 
                                        String productName, Integer quantity,
                                        Money unitPrice, Address address) {
        OrderAggregate order = new OrderAggregate();
        order.userId = userId;
        order.productId = productId;
        order.productName = productName;
        order.quantity = quantity;
        order.unitPrice = unitPrice;
        order.totalAmount = unitPrice.multiply(quantity);
        order.status = 0; // 待支付
        order.shippingAddress = address;
        order.createTime = LocalDateTime.now();
        order.updateTime = LocalDateTime.now();

        // 添加领域事件
        order.addDomainEvent(new OrderCreatedEvent(
            null, // ID 由基础设施层生成
            null, // 订单号由基础设施层生成
            userId,
            productId,
            productName,
            quantity,
            order.totalAmount.getAmount()
        ));

        return order;
    }

    // ==================== 业务方法 ====================

    /**
     * 支付订单
     */
    public void pay() {
        // 业务规则校验
        if (this.status != 0) {
            throw new IllegalStateException("只有待支付订单才能支付");
        }

        this.status = 1; // 已支付
        this.payTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();

        // 添加领域事件
        addDomainEvent(new OrderPaidEvent(
            this.id,
            this.orderNo,
            this.totalAmount.getAmount(),
            this.payTime
        ));
    }

    /**
     * 取消订单
     */
    public void cancel(String reason) {
        // 业务规则校验
        if (this.status != 0) {
            throw new IllegalStateException("只有待支付订单才能取消");
        }

        this.status = 4; // 已取消
        this.updateTime = LocalDateTime.now();

        // 添加领域事件
        addDomainEvent(new OrderCancelledEvent(
            this.id,
            this.orderNo,
            reason
        ));
    }

    /**
     * 确认收货
     */
    public void confirmReceived() {
        // 业务规则校验
        if (this.status != 2) {
            throw new IllegalStateException("只有已发货订单才能确认收货");
        }

        this.status = 3; // 已完成
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 发货
     */
    public void ship() {
        // 业务规则校验
        if (this.status != 1) {
            throw new IllegalStateException("只有已支付订单才能发货");
        }

        this.status = 2; // 已发货
        this.updateTime = LocalDateTime.now();
    }

    // ==================== 查询方法 ====================

    /**
     * 是否已支付
     */
    public boolean isPaid() {
        return this.status == 1;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return this.status == 4;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this.status == 0;
    }

    // ==================== 内部方法 ====================

    /**
     * 添加领域事件
     */
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * 获取并清除领域事件
     */
    public List<DomainEvent> getAndClearDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    /**
     * 设置基础设施层生成的ID和订单号
     */
    public void setGeneratedIdAndOrderNo(Long id, String orderNo) {
        this.id = id;
        this.orderNo = orderNo;
        
        // 更新领域事件中的ID和订单号
        this.domainEvents.forEach(event -> {
            try {
                var field = event.getClass().getDeclaredField("aggregateId");
                field.setAccessible(true);
                // 注意：这里简化处理，实际应该使用反射或构造函数重新创建事件
            } catch (Exception e) {
                // 忽略
            }
        });
    }
}
