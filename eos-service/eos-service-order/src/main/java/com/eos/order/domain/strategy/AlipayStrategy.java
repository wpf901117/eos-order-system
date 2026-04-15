package com.eos.order.domain.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 支付宝支付策略
 */
@Component
@Slf4j
public class AlipayStrategy implements PaymentStrategy {

    @Override
    public String pay(String orderNo, BigDecimal amount) {
        log.info("[支付宝支付] 订单号: {}, 金额: {}", orderNo, amount);
        
        // TODO: 调用支付宝 SDK
        // AlipayClient alipayClient = new DefaultAlipayClient(...);
        // AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // ...
        
        // 模拟支付成功
        String paymentNo = "ALIPAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[支付宝支付] 支付成功，流水号: {}", paymentNo);
        
        return paymentNo;
    }

    @Override
    public String getPaymentType() {
        return "ALIPAY";
    }
}
