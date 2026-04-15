package com.eos.order.service.impl;

import com.eos.common.exception.BizException;
import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import com.eos.order.dto.OrderCreateDTO;
import com.eos.order.entity.Order;
import com.eos.order.feign.ProductFeignClient;
import com.eos.order.mapper.OrderMapper;
import com.eos.order.mq.OrderTimeoutProducer;
import com.eos.order.vo.OrderVO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 *
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>订单创建：金额校验、权限校验</li>
 *   <li>订单支付：状态校验</li>
 *   <li>订单取消：所有权校验、库存回滚</li>
 *   <li>超时取消：库存回滚</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private OrderTimeoutProducer orderTimeoutProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private ProductVO mockProduct;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        // Mock 商品
        mockProduct = new ProductVO();
        mockProduct.setId(2000000000000000001L);
        mockProduct.setName("iPhone 15 Pro");
        mockProduct.setPrice(new BigDecimal("7999.00"));
        mockProduct.setStatus(1);
        mockProduct.setStock(100);

        // Mock 订单
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNo("260415120000123456");
        mockOrder.setUserId(1000000000000000001L);
        mockOrder.setProductId(2000000000000000001L);
        mockOrder.setProductName("iPhone 15 Pro");
        mockOrder.setQuantity(1);
        mockOrder.setUnitPrice(new BigDecimal("7999.00"));
        mockOrder.setTotalAmount(new BigDecimal("7999.00"));
        mockOrder.setStatus(0);
        mockOrder.setAddress("北京市海淀区");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("订单创建测试")
    class CreateOrderTests {

        @Test
        @DisplayName("金额校验失败应抛出异常")
        void shouldRejectWrongAmount() {
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(1);
            dto.setTotalAmount(new BigDecimal("9999.00")); // 错误的金额
            dto.setAddress("北京市海淀区");

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(productFeignClient.getProductById(2000000000000000001L))
                    .thenReturn(Result.ok(mockProduct));

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.createOrder(1000000000000000001L, dto));
            assertEquals(ResultCode.PAY_AMOUNT_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("下架商品应拒绝下单")
        void shouldRejectOffShelfProduct() {
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(1);
            dto.setTotalAmount(new BigDecimal("7999.00"));
            dto.setAddress("北京市海淀区");

            mockProduct.setStatus(0); // 下架

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(productFeignClient.getProductById(2000000000000000001L))
                    .thenReturn(Result.ok(mockProduct));

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.createOrder(1000000000000000001L, dto));
            assertEquals(ResultCode.PRODUCT_OFF_SHELF.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("库存不足应拒绝下单")
        void shouldRejectWhenStockNotEnough() {
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(10);
            dto.setTotalAmount(new BigDecimal("79990.00"));
            dto.setAddress("北京市海淀区");

            mockProduct.setStock(5); // 库存不足

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(productFeignClient.getProductById(2000000000000000001L))
                    .thenReturn(Result.ok(mockProduct));

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.createOrder(1000000000000000001L, dto));
            assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("重复下单应拒绝")
        void shouldRejectRepeatSubmit() {
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(1);
            dto.setTotalAmount(new BigDecimal("7999.00"));
            dto.setAddress("北京市海淀区");

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(false); // 获取锁失败 = 重复提交

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.createOrder(1000000000000000001L, dto));
            assertEquals(ResultCode.ORDER_REPEAT_SUBMIT.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("订单所有权校验测试")
    class OrderAccessControlTests {

        @BeforeEach
        void setUp() {
            when(orderMapper.selectById(1L)).thenReturn(mockOrder);
        }

        @Test
        @DisplayName("普通用户不能查看他人订单")
        void shouldDenyOtherUserOrder() {
            BizException ex = assertThrows(BizException.class,
                    () -> orderService.getOrderById(1L, 999999999999999999L, "USER"));
            assertEquals(ResultCode.ORDER_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("普通用户可以查看自己的订单")
        void shouldAllowOwnerAccess() {
            OrderVO result = orderService.getOrderById(1L, 1000000000000000001L, "USER");
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("管理员可以查看任意订单")
        void shouldAllowAdminAccess() {
            OrderVO result = orderService.getOrderById(1L, 888888888888888888L, "ADMIN");
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("普通用户不能取消他人订单")
        void shouldDenyCancelOtherOrder() {
            BizException ex = assertThrows(BizException.class,
                    () -> orderService.cancelOrder(1L, 999999999999999999L, "USER"));
            assertEquals(ResultCode.ORDER_ACCESS_DENIED.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("订单状态流转测试")
    class OrderStatusTransitionTests {

        @BeforeEach
        void setUp() {
            when(orderMapper.selectById(1L)).thenReturn(mockOrder);
        }

        @Test
        @DisplayName("已支付订单不能再取消")
        void shouldRejectCancelPaidOrder() {
            mockOrder.setStatus(1); // 已支付

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.cancelOrder(1L, 1000000000000000001L, "USER"));
            assertEquals(ResultCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("未支付订单不能发货")
        void shouldRejectShipUnpaidOrder() {
            BizException ex = assertThrows(BizException.class,
                    () -> orderService.shipOrder(1L, "ADMIN"));
            assertEquals(ResultCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("管理员可以发货已支付订单")
        void shouldAllowShipPaidOrder() {
            mockOrder.setStatus(1); // 已支付

            OrderVO result = orderService.shipOrder(1L, "ADMIN");
            assertNotNull(result);
            assertEquals(2, result.getStatus()); // 已发货
        }

        @Test
        @DisplayName("非管理员不能发货")
        void shouldDenyShipByNonAdmin() {
            mockOrder.setStatus(1);

            BizException ex = assertThrows(BizException.class,
                    () -> orderService.shipOrder(1L, "USER"));
            assertEquals(ResultCode.PERMISSION_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("未发货订单不能确认收货")
        void shouldRejectConfirmUnshippedOrder() {
            BizException ex = assertThrows(BizException.class,
                    () -> orderService.confirmOrder(1L, 1000000000000000001L, "USER"));
            assertEquals(ResultCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("库存回滚测试")
    class StockRollbackTests {

        @BeforeEach
        void setUp() {
            when(orderMapper.selectById(1L)).thenReturn(mockOrder);
        }

        @Test
        @DisplayName("取消订单应调用库存回滚")
        void shouldRollbackStockOnCancel() {
            mockOrder.setProductId(2000000000000000001L);
            mockOrder.setQuantity(2);

            when(productFeignClient.rollbackStock(any(StockDeductDTO.class)))
                    .thenReturn(Result.ok(true));

            orderService.cancelOrder(1L, 1000000000000000001L, "USER");

            verify(productFeignClient).rollbackStock(argThat(dto ->
                    dto.getProductId().equals(2000000000000000001L)
                            && dto.getQuantity() == 2));
        }

        @Test
        @DisplayName("超时取消订单应调用库存回滚")
        void shouldRollbackStockOnTimeoutCancel() {
            mockOrder.setProductId(2000000000000000001L);
            mockOrder.setQuantity(3);

            when(productFeignClient.rollbackStock(any(StockDeductDTO.class)))
                    .thenReturn(Result.ok(true));

            orderService.timeoutCancelOrder(1L);

            verify(productFeignClient).rollbackStock(argThat(dto ->
                    dto.getProductId().equals(2000000000000000001L)
                            && dto.getQuantity() == 3));
        }

        @Test
        @DisplayName("超时取消非待支付订单不调用回滚")
        void shouldNotRollbackNonPendingOrder() {
            mockOrder.setStatus(1); // 已支付

            orderService.timeoutCancelOrder(1L);

            verify(productFeignClient, never()).rollbackStock(any());
        }
    }
}
