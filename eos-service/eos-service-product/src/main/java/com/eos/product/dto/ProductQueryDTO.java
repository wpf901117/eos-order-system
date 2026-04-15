package com.eos.product.dto;

import lombok.Data;

/**
 * 商品查询DTO
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class ProductQueryDTO {

    /** 关键字（匹配名称） */
    private String keyword;

    /** 分类ID */
    private Long categoryId;

    /** 状态：0-下架，1-上架 */
    private Integer status;
}
