package com.eos.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 商品图片URL */
    private String imageUrl;

    /** 分类ID */
    private Long categoryId;

    /** 状态：0-下架，1-上架 */
    private Integer status;

    /** 销量 */
    private Integer sales;

    /** 创建时间 */
    private LocalDateTime createTime;
}
