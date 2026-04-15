package com.eos.product.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 库存扣减DTO
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class StockDeductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于0")
    private Integer quantity;
}
