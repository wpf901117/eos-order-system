package com.eos.product.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品更新DTO
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class ProductUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称不能超过128字符")
    private String name;

    @Size(max = 2000, message = "商品描述不能超过2000字符")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    @Size(max = 255, message = "图片URL不能超过255字符")
    private String imageUrl;

    private Long categoryId;

    /** 状态：0-下架，1-上架 */
    private Integer status;
}
