package com.eos.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存（乐观锁实现）
     *
     * <p>通过 WHERE stock >= quantity 条件实现原子性扣减，
     * 防止并发场景下的超卖问题。</p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 影响行数，0表示库存不足
     */
    @Update("UPDATE t_product SET stock = stock - #{quantity}, sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND status = 1 AND deleted = 0")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回滚库存
     *
     * @param productId 商品ID
     * @param quantity  回滚数量
     * @return 影响行数
     */
    @Update("UPDATE t_product SET stock = stock + #{quantity}, sales = sales - #{quantity} " +
            "WHERE id = #{productId} AND deleted = 0")
    int rollbackStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
