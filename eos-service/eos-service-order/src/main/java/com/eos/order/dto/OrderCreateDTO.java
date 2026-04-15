package com.eos.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建DTO
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 前端传入的总金额，用于服务端校验 */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal totalAmount;
}
