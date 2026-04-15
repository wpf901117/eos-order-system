package com.eos.order.domain.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 微信支付策略
 */
@Component
@Slf4j
public class WechatPayStrategy implements PaymentStrategy {

    @Override
    public String pay(String orderNo, BigDecimal amount) {
        log.info("[微信支付] 订单号: {}, 金额: {}", orderNo, amount);
        
        // TODO: 调用微信支付 SDK
        
        String paymentNo = "WECHAT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[微信支付] 支付成功，流水号: {}", paymentNo);
        
        return paymentNo;
    }

    @Override
    public String getPaymentType() {
        return "WECHAT";
    }
}
