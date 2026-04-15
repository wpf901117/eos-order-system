package com.eos.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
