package com.eos.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异明细实体
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_reconciliation_diff")
public class ReconciliationDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对账记录ID */
    private Long reconciliationId;

    /** 订单号 */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 支付流水号 */
    private String paymentNo;

    /** 支付金额 */
    private BigDecimal paymentAmount;

    /** 差异类型：1-订单有支付无，2-支付有订单无，3-金额不一致 */
    private Integer diffType;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 处理状态：0-未处理，1-已处理 */
    private Integer handleStatus;

    /** 处理备注 */
    private String handleRemark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
