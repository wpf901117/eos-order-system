package com.eos.order.feign;

import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 商品服务Feign降级处理
 *
 * <p>当商品服务不可用时，OpenFeign会自动调用Fallback实现，
 * 避免订单服务因等待商品服务响应而被拖垮，这是防止服务雪崩的关键手段。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public Result<ProductVO> getProductById(Long productId) {
        log.error("[Feign降级] 商品服务不可用，getProductById productId={}", productId);
        return Result.error(ResultCode.RPC_ERROR, "商品服务暂不可用，请稍后再试");
    }

    @Override
    public Result<Boolean> deductStock(StockDeductDTO dto) {
        log.error("[Feign降级] 商品服务不可用，deductStock productId={}", dto.getProductId());
        return Result.error(ResultCode.RPC_ERROR, "库存服务暂不可用，请稍后再试");
    }

    @Override
    public Result<Integer> getStock(Long productId) {
        log.error("[Feign降级] 商品服务不可用，getStock productId={}", productId);
        return Result.ok(0);
    }

    @Override
    public Result<Boolean> rollbackStock(StockDeductDTO dto) {
        log.error("[Feign降级] 商品服务不可用，rollbackStock productId={}", dto.getProductId());
        return Result.error(ResultCode.RPC_ERROR, "库存服务暂不可用，请稍后再试");
    }
}
