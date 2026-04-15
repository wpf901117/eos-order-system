package com.eos.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eos.product.dto.ProductCreateDTO;
import com.eos.product.dto.ProductQueryDTO;
import com.eos.product.dto.ProductUpdateDTO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.entity.Product;
import com.eos.product.vo.ProductVO;

import java.util.List;

/**
 * 商品服务接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
public interface ProductService {

    /**
     * 根据ID查询商品
     */
    ProductVO getProductById(Long productId);

    /**
     * 查询商品列表（公开）
     */
    List<ProductVO> listProducts();

    /**
     * 分页查询商品（管理员）
     */
    IPage<ProductVO> pageProducts(ProductQueryDTO query, long pageNo, long pageSize);

    /**
     * 创建商品（管理员）
     */
    ProductVO createProduct(ProductCreateDTO dto);

    /**
     * 更新商品（管理员）
     */
    ProductVO updateProduct(Long productId, ProductUpdateDTO dto);

    /**
     * 删除商品（管理员）
     */
    void deleteProduct(Long productId);

    /**
     * 上下架商品（管理员）
     */
    void updateStatus(Long productId, Integer status);

    /**
     * 扣减库存
     */
    boolean deductStock(StockDeductDTO dto);

    /**
     * 回滚库存
     */
    boolean rollbackStock(StockDeductDTO dto);

    /**
     * 获取商品库存（用于内部调用）
     */
    Integer getStock(Long productId);
}
