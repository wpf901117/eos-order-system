package com.eos.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录实体
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_reconciliation")
public class Reconciliation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对账日期 */
    private LocalDate reconciliationDate;

    /** 订单总数 */
    private Integer totalOrderCount;

    /** 订单总金额 */
    private BigDecimal totalOrderAmount;

    /** 支付流水总数 */
    private Integer totalPaymentCount;

    /** 支付流水总金额 */
    private BigDecimal totalPaymentAmount;

    /** 差异数量 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 对账状态：0-待对账，1-对账中，2-对账成功，3-对账失败 */
    private Integer status;

    /** 对账报告路径 */
    private String reportPath;

    /** 错误信息 */
    private String errorMsg;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
