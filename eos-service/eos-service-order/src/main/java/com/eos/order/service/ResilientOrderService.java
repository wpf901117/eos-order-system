package com.eos.order.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import com.eos.order.dto.OrderCreateDTO;
import com.eos.order.feign.ProductFeignClient;
import com.eos.order.vo.OrderVO;
import com.eos.product.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 带熔断降级的订单服务示例
 *
 * <p>展示如何使用 Sentinel 实现：</p>
 * <ul>
 *   <li>流量控制（限流）</li>
 *   <li>熔断降级（服务不可用时快速失败）</li>
 *   <li>热点参数限流</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class ResilientOrderService {

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 创建订单（带流量控制和熔断降级）
     *
     * @param userId 用户ID
     * @param dto 订单创建DTO
     * @return 订单VO
     */
    @SentinelResource(
        value = "createOrder",
        blockHandler = "handleBlockException",
        fallback = "handleFallback"
    )
    public OrderVO createOrderWithResilience(Long userId, OrderCreateDTO dto) {
        log.info("[弹性服务] 开始创建订单，userId={}, productId={}", userId, dto.getProductId());

        // 调用商品服务（可能触发熔断）
        ProductVO product = getProductWithFallback(dto.getProductId());

        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 业务逻辑...
        log.info("[弹性服务] 订单创建成功");
        return null; // 简化示例
    }

    /**
     * 查询商品（带熔断降级）
     *
     * @param productId 商品ID
     * @return 商品VO
     */
    @SentinelResource(
        value = "getProductById",
        blockHandler = "handleProductBlockException",
        fallback = "handleProductFallback"
    )
    public ProductVO getProductWithFallback(Long productId) {
        log.debug("[弹性服务] 查询商品，productId={}", productId);
        
        var result = productFeignClient.getProductById(productId);
        
        if (result != null && result.isSuccess()) {
            return result.getData();
        }
        
        return null;
    }

    // ==================== 流控/降级处理方法 ====================

    /**
     * 订单创建 - 流控处理
     *
     * @param userId 用户ID
     * @param dto 订单DTO
     * @param ex 异常
     * @return 降级结果
     */
    public OrderVO handleBlockException(Long userId, OrderCreateDTO dto, BlockException ex) {
        log.warn("[流控] 订单创建接口被限流，userId={}", userId);
        throw new BizException(ResultCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后再试");
    }

    /**
     * 订单创建 - 降级处理（业务异常）
     *
     * @param userId 用户ID
     * @param dto 订单DTO
     * @param t 异常
     * @return 降级结果
     */
    public OrderVO handleFallback(Long userId, OrderCreateDTO dto, Throwable t) {
        log.error("[降级] 订单创建失败，userId={}", userId, t);
        throw new BizException(ResultCode.INTERNAL_SERVER_ERROR, "订单创建失败，请稍后重试");
    }

    /**
     * 商品查询 - 流控处理
     *
     * @param productId 商品ID
     * @param ex 异常
     * @return 降级结果
     */
    public ProductVO handleProductBlockException(Long productId, BlockException ex) {
        log.warn("[流控] 商品查询接口被限流，productId={}", productId);
        return null;
    }

    /**
     * 商品查询 - 降级处理（服务不可用）
     *
     * @param productId 商品ID
     * @param t 异常
     * @return 降级结果（返回缓存或默认值）
     */
    public ProductVO handleProductFallback(Long productId, Throwable t) {
        log.error("[降级] 商品服务不可用，productId={}", productId, t);
        
        // 降级策略：返回缓存数据或默认值
        // 这里可以集成 Redis 缓存，返回过期但仍可用的数据
        return getDefaultProduct(productId);
    }

    /**
     * 获取默认商品（降级策略）
     *
     * @param productId 商品ID
     * @return 默认商品
     */
    private ProductVO getDefaultProduct(Long productId) {
        // 实际项目中可以从本地缓存或 Redis 获取
        log.warn("[降级] 使用默认商品信息，productId={}", productId);
        
        ProductVO defaultProduct = new ProductVO();
        defaultProduct.setId(productId);
        defaultProduct.setName("商品信息暂时不可用");
        defaultProduct.setStatus(0); // 下架状态
        
        return defaultProduct;
    }
}
