package com.eos.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eos.common.constant.CommonConstant;
import com.eos.common.exception.BizException;
import com.eos.common.result.PageResult;
import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import com.eos.common.util.DistributedLockUtil;
import com.eos.common.util.SnowflakeUtil;
import com.eos.order.annotation.AuditLog;
import com.eos.order.dto.OrderCreateDTO;
import com.eos.order.entity.Order;
import com.eos.order.cache.BloomFilterService;
import com.eos.order.cache.MultiLevelCache;
import com.eos.order.event.DomainEventBus;
import com.eos.order.event.events.OrderCreatedEvent;
import com.eos.order.event.events.OrderPaidEvent;
import com.eos.order.feign.ProductFeignClient;
import com.eos.order.mapper.OrderMapper;
import com.eos.order.mq.OrderTimeoutProducer;
import com.eos.order.monitor.OrderMetrics;
import com.eos.order.service.OrderService;
import com.eos.order.vo.OrderVO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.vo.ProductVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 *
 * <p>核心业务类，展示多个高级技术点：</p>
 * <ul>
 *   <li><strong>分布式事务</strong>：@GlobalTransactional 保证跨服务的订单创建和库存扣减一致性</li>
 *   <li><strong>分布式锁</strong>：Redis 防止重复提交订单</li>
 *   <li><strong>幂等性</strong>：基于 userId + productId 实现幂等下单</li>
 *   <li><strong>消息队列</strong>：RocketMQ 发送延迟消息实现订单超时取消</li>
 *   <li><strong>Feign调用</strong>：跨服务调用商品服务扣减/回滚库存</li>
 *   <li><strong>权限控制</strong>：订单所有权校验，管理员可例外操作</li>
 *   <li><strong>状态机</strong>：待支付 -> 已支付 -> 已发货 -> 已完成 / 已取消</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderTimeoutProducer orderTimeoutProducer;

    @Autowired
    private OrderMetrics orderMetrics;

    @Autowired
    private DomainEventBus eventBus;

    @Autowired
    private MultiLevelCache multiLevelCache;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Override
    @AuditLog(module = "订单管理", operation = "创建订单")
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        long startTime = System.currentTimeMillis();
        
        // 1. 使用分布式锁防止超卖和重复提交
        String lockKey = "order:create:" + userId + ":" + dto.getProductId();
        
        return DistributedLockUtil.executeWithLock(redissonClient, lockKey, () -> {
            try {
                // 2. 查询商品信息
                Result<ProductVO> productResult = productFeignClient.getProductById(dto.getProductId());
                if (productResult == null || !productResult.isSuccess() || productResult.getData() == null) {
                    throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
                }
                ProductVO product = productResult.getData();
                if (product.getStatus() != 1) {
                    throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
                }

                // 3. 金额校验
                BigDecimal expectedAmount = product.getPrice().multiply(new BigDecimal(dto.getQuantity()));
                if (expectedAmount.compareTo(dto.getTotalAmount()) != 0) {
                    log.warn("[订单创建] 金额校验失败，期望金额={}，实际金额={}", expectedAmount, dto.getTotalAmount());
                    throw new BizException(ResultCode.PAY_AMOUNT_ERROR, "订单金额校验失败");
                }

                // 4. 扣减库存（分布式事务保证一致性）
                StockDeductDTO stockDeductDTO = new StockDeductDTO();
                stockDeductDTO.setProductId(dto.getProductId());
                stockDeductDTO.setQuantity(dto.getQuantity());
                Result<Boolean> deductResult = productFeignClient.deductStock(stockDeductDTO);
                if (deductResult == null || !deductResult.isSuccess() || !Boolean.TRUE.equals(deductResult.getData())) {
                    throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
                }

                // 5. 生成订单
                Order order = new Order();
                order.setId(SnowflakeUtil.getInstance().nextId());
                order.setOrderNo(generateOrderNo());
                order.setUserId(userId);
                order.setProductId(dto.getProductId());
                order.setProductName(product.getName());
                order.setQuantity(dto.getQuantity());
                order.setUnitPrice(product.getPrice());
                order.setTotalAmount(expectedAmount);
                order.setStatus(0); // 待支付
                order.setAddress(dto.getAddress());
                orderMapper.insert(order);

                // 6.1 添加订单ID到布隆过滤器
                bloomFilterService.addOrder(order.getId());

                // 6.2 发送延迟消息，30分钟后检查订单状态
                orderTimeoutProducer.sendDelayMessage(order.getId(), 30);

                // 7. 发布领域事件（异步）
                OrderCreatedEvent createdEvent = new OrderCreatedEvent(
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getProductId(),
                    order.getProductName(),
                    order.getQuantity(),
                    order.getTotalAmount()
                );
                eventBus.publishAsync(createdEvent);

                // 8. 记录监控指标
                long duration = System.currentTimeMillis() - startTime;
                orderMetrics.recordOrderCreate();
                orderMetrics.recordOrderCreateDuration(duration);

                log.info("[订单创建] 订单创建成功，orderNo={}，userId={}，amount={}，耗时={}ms",
                        order.getOrderNo(), order.getUserId(), order.getTotalAmount(), duration);

                return convertToVO(order);
            } catch (Exception e) {
                log.error("[订单创建] 订单创建失败，userId={}, productId={}", userId, dto.getProductId(), e);
                throw e;
            }
        });
    }

    @Override
    public OrderVO getOrderById(Long orderId, Long userId, String role) {
        // 1. 布隆过滤器检查（防止缓存穿透）
        if (!bloomFilterService.mightContainOrder(orderId)) {
            log.debug("[布隆过滤器] 订单ID不存在，orderId={}", orderId);
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }

        // 2. 多级缓存查询
        String cacheKey = "order:" + orderId;
        Order order = multiLevelCache.get(cacheKey, Order.class, () -> {
            Order dbOrder = orderMapper.selectById(orderId);
            if (dbOrder != null) {
                // 添加到布隆过滤器
                bloomFilterService.addOrder(dbOrder.getId());
            }
            return dbOrder;
        });

        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }

        // 3. 权限校验：普通用户只能查看自己的订单
        if (!CommonConstant.ROLE_ADMIN.equals(role) && !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }

        return convertToVO(order);
    }

    @Override
    public PageResult<OrderVO> listCurrentUserOrders(Long userId, Long pageNo, Long pageSize,
                                                     Integer status, String orderNo) {
        Page<Order> page = buildOrderPage(pageNo, pageSize, status, orderNo, userId, null);
        return PageResult.of(page, page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
    }

    @Override
    public PageResult<OrderVO> listOrders(Long pageNo, Long pageSize,
                                          Integer status, String orderNo, Long userId, String role) {
        // 管理员可以查全部，普通用户只能查自己
        Long effectiveUserId = CommonConstant.ROLE_ADMIN.equals(role) ? null : userId;
        Page<Order> page = buildOrderPage(pageNo, pageSize, status, orderNo, effectiveUserId, userId);
        return PageResult.of(page, page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
    }

    private Page<Order> buildOrderPage(Long pageNo, Long pageSize, Integer status,
                                        String orderNo, Long userId, Long callerUserId) {
        // 未登录或普通用户调用管理接口，拒绝
        if (callerUserId != null && userId == null) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Page<Order> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.eq(Order::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    @AuditLog(module = "订单管理", operation = "支付订单")
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!CommonConstant.ROLE_ADMIN.equals(role) && !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (order.getStatus() != 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 发布领域事件
        OrderPaidEvent paidEvent = new OrderPaidEvent(
            order.getId(),
            order.getOrderNo(),
            order.getTotalAmount(),
            order.getPayTime()
        );
        eventBus.publishAsync(paidEvent);

        // 记录监控指标
        orderMetrics.recordOrderPaySuccess();

        log.info("[订单支付] 订单支付成功，orderId={}，orderNo={}", orderId, order.getOrderNo());
        return convertToVO(order);
    }

    @Override
    @AuditLog(module = "订单管理", operation = "取消订单")
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!CommonConstant.ROLE_ADMIN.equals(role) && !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (order.getStatus() != 0) {
            log.warn("[订单取消] 订单状态不允许取消，orderId={}，status={}", orderId, order.getStatus());
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 1. 更新订单状态为已取消
        order.setStatus(4);
        orderMapper.updateById(order);

        // 2. 回滚库存
        rollbackStock(order.getProductId(), order.getQuantity());

        // 3. 记录监控指标
        orderMetrics.recordOrderCancel();

        log.info("[订单取消] 订单已取消，orderId={}，orderNo={}", orderId, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO shipOrder(Long orderId, String role) {
        if (!CommonConstant.ROLE_ADMIN.equals(role)) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(2); // 已发货
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("[订单发货] 订单已发货，orderId={}，orderNo={}", orderId, order.getOrderNo());
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirmOrder(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!CommonConstant.ROLE_ADMIN.equals(role) && !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (order.getStatus() != 2) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(3); // 已完成
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("[确认收货] 订单已完成，orderId={}，orderNo={}", orderId, order.getOrderNo());
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void timeoutCancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("[超时取消] 订单不存在，orderId={}", orderId);
            return;
        }
        if (order.getStatus() != 0) {
            log.info("[超时取消] 订单状态不是待支付，无需取消，orderId={}，status={}", orderId, order.getStatus());
            return;
        }

        order.setStatus(4);
        orderMapper.updateById(order);

        // 回滚库存
        rollbackStock(order.getProductId(), order.getQuantity());

        // 记录监控指标
        orderMetrics.recordOrderTimeout();

        log.info("[超时取消] 订单已超时取消，orderId={}，orderNo={}", orderId, order.getOrderNo());
    }

    /**
     * 通过Feign回滚库存
     */
    private void rollbackStock(Long productId, Integer quantity) {
        StockDeductDTO rollbackDTO = new StockDeductDTO();
        rollbackDTO.setProductId(productId);
        rollbackDTO.setQuantity(quantity);
        Result<Boolean> result = productFeignClient.rollbackStock(rollbackDTO);
        if (result == null || !result.isSuccess()) {
            log.error("[库存回滚] 库存回滚失败，productId={}，quantity={}", productId, quantity);
        } else {
            log.info("[库存回滚] 库存已回滚，productId={}，quantity={}", productId, quantity);
        }
    }

    private String generateOrderNo() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String seq = String.valueOf(SnowflakeUtil.getInstance().nextId() % 1000000);
        return datePrefix + String.format("%06d", Integer.parseInt(seq));
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setQuantity(order.getQuantity());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setAddress(order.getAddress());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知状态";
        }
    }
}
