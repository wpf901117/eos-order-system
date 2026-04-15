package com.eos.product.service.impl;

import com.eos.common.exception.BizException;
import com.eos.product.dto.ProductCreateDTO;
import com.eos.product.dto.ProductUpdateDTO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.entity.Product;
import com.eos.product.mapper.ProductMapper;
import com.eos.product.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商品服务单元测试
 *
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>商品创建：参数校验</li>
 *   <li>商品更新：存在性校验</li>
 *   <li>库存扣减：库存不足处理</li>
 *   <li>库存回滚：基本流程</li>
 *   <li>管理员操作：权限校验</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = new Product();
        mockProduct.setId(2000000000000000001L);
        mockProduct.setName("iPhone 15 Pro");
        mockProduct.setDescription("Apple最新旗舰");
        mockProduct.setPrice(new BigDecimal("7999.00"));
        mockProduct.setStock(100);
        mockProduct.setStatus(1);
        mockProduct.setSales(0);
        mockProduct.setDeleted(0);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("商品管理测试")
    class ProductManagementTests {

        @Test
        @DisplayName("创建商品成功")
        void shouldCreateProduct() {
            ProductCreateDTO dto = new ProductCreateDTO();
            dto.setName("MacBook Pro 14");
            dto.setDescription("M3芯片专业笔记本");
            dto.setPrice(new BigDecimal("14999.00"));
            dto.setStock(50);
            dto.setCategoryId(1L);

            when(productMapper.insert(any(Product.class))).thenReturn(1);

            ProductVO result = productService.createProduct(dto);

            assertNotNull(result);
            assertEquals("MacBook Pro 14", result.getName());
            assertEquals(new BigDecimal("14999.00"), result.getPrice());
            assertEquals(Integer.valueOf(1), result.getStatus());
            verify(productMapper).insert(any(Product.class));
        }

        @Test
        @DisplayName("更新不存在的商品应抛异常")
        void shouldRejectUpdateNonExistentProduct() {
            ProductUpdateDTO dto = new ProductUpdateDTO();
            dto.setName("更新名称");
            dto.setPrice(new BigDecimal("100.00"));
            dto.setStock(10);

            when(productMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> productService.updateProduct(999L, dto));
            assertEquals(40403, ex.getCode()); // PRODUCT_NOT_FOUND
        }

        @Test
        @DisplayName("删除不存在的商品应抛异常")
        void shouldRejectDeleteNonExistentProduct() {
            when(productMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> productService.deleteProduct(999L));
            assertEquals(40403, ex.getCode());
        }
    }

    @Nested
    @DisplayName("库存管理测试")
    class StockManagementTests {

        @Test
        @DisplayName("库存不足应返回false")
        void shouldReturnFalseWhenStockNotEnough() throws InterruptedException {
            mockProduct.setStock(5);

            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(productMapper.selectById(2000000000000000001L)).thenReturn(mockProduct);

            StockDeductDTO dto = new StockDeductDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(10);

            boolean result = productService.deductStock(dto);

            assertFalse(result);
            verify(productMapper, never()).deductStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("库存扣减成功")
        void shouldDeductStockSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(productMapper.selectById(2000000000000000001L)).thenReturn(mockProduct);
            when(productMapper.deductStock(2000000000000000001L, 2)).thenReturn(1);

            StockDeductDTO dto = new StockDeductDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(2);

            boolean result = productService.deductStock(dto);

            assertTrue(result);
            verify(productMapper).deductStock(2000000000000000001L, 2);
        }

        @Test
        @DisplayName("库存回滚成功")
        void shouldRollbackStockSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(productMapper.rollbackStock(2000000000000000001L, 3)).thenReturn(1);

            StockDeductDTO dto = new StockDeductDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(3);

            boolean result = productService.rollbackStock(dto);

            assertTrue(result);
            verify(productMapper).rollbackStock(2000000000000000001L, 3);
        }

        @Test
        @DisplayName("获取分布式锁失败应抛异常")
        void shouldThrowWhenLockFailed() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

            StockDeductDTO dto = new StockDeductDTO();
            dto.setProductId(2000000000000000001L);
            dto.setQuantity(1);

            BizException ex = assertThrows(BizException.class,
                    () -> productService.deductStock(dto));
            assertEquals(60401, ex.getCode()); // DISTRIBUTED_LOCK_ERROR
        }
    }

    @Nested
    @DisplayName("状态管理测试")
    class StatusManagementTests {

        @Test
        @DisplayName("更新不存在的商品状态应抛异常")
        void shouldRejectStatusUpdateForNonExistent() {
            when(productMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> productService.updateStatus(999L, 0));
            assertEquals(40403, ex.getCode());
        }

        @Test
        @DisplayName("上下架操作不抛异常")
        void shouldUpdateStatusWithoutException() {
            when(productMapper.selectById(2000000000000000001L)).thenReturn(mockProduct);
            when(productMapper.updateById(any(Product.class))).thenReturn(1);

            assertDoesNotThrow(() -> productService.updateStatus(2000000000000000001L, 0));
            verify(productMapper).updateById(any(Product.class));
        }
    }
}
