package com.eos.order.feign;

import com.eos.common.result.Result;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.vo.ProductVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 商品服务Feign客户端
 *
 * <p>OpenFeign 是 Spring Cloud 提供的声明式 HTTP 客户端，让我们可以像调用本地方法一样调用远程服务。</p>
 *
 * <p><strong>核心优势：</strong></p>
 * <ul>
 *   <li>无需手写 HTTP 请求代码</li>
 *   <li>集成 Ribbon 实现负载均衡</li>
 *   <li>集成 Hystrix/Sentinel 实现熔断降级</li>
 *   <li>支持请求/响应压缩</li>
 * </ul>
 *
 * <p><strong>最佳实践：</strong></p>
 * <ul>
 *   <li>Feign接口应单独放到 API 模块中，供调用方引用</li>
 *   <li>必须配置 Fallback 降级逻辑，防止服务雪崩</li>
 *   <li>超时时间要根据业务合理设置</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@FeignClient(name = "eos-product-service", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {

    /**
     * 查询商品详情
     *
     * @param productId 商品ID
     * @return 商品信息
     */
    @GetMapping("/product/{productId}")
    Result<ProductVO> getProductById(@PathVariable("productId") Long productId);

    /**
     * 扣减库存
     *
     * @param dto 库存扣减信息
     * @return 是否成功
     */
    @PostMapping("/product/deductStock")
    Result<Boolean> deductStock(@RequestBody StockDeductDTO dto);

    /**
     * 查询库存
     *
     * @param productId 商品ID
     * @return 库存数量
     */
    @GetMapping("/product/{productId}/stock")
    Result<Integer> getStock(@PathVariable("productId") Long productId);

    /**
     * 回滚库存
     *
     * @param dto 库存回滚信息
     * @return 是否成功
     */
    @PostMapping("/product/rollbackStock")
    Result<Boolean> rollbackStock(@RequestBody StockDeductDTO dto);
}
