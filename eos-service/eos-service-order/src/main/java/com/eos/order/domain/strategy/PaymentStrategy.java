package com.eos.order.domain.strategy;

import java.math.BigDecimal;

/**
 * 支付策略接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
public interface PaymentStrategy {

    /**
     * 执行支付
     *
     * @param orderNo 订单号
     * @param amount 支付金额
     * @return 支付流水号
     */
    String pay(String orderNo, BigDecimal amount);

    /**
     * 获取支付方式名称
     */
    String getPaymentType();
}
