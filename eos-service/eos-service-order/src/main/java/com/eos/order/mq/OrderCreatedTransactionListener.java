package com.eos.order.mq;

import com.eos.order.entity.Order;
import com.eos.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 订单创建事务监听器
 *
 * <p>RocketMQ 事务消息的本地事务执行器和回查器。</p>
 *
 * <p><strong>工作流程：</strong></p>
 * <ol>
 *   <li>发送半消息（Half Message）到 MQ</li>
 *   <li>执行本地事务（创建订单）</li>
 *   <li>根据本地事务结果提交或回滚消息</li>
 *   <li>如果 MQ 未收到确认，会主动回查本地事务状态</li>
 * </ol>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@RocketMQTransactionListener
@Component
@Slf4j
public class OrderCreatedTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 执行本地事务
     *
     * @param msg 消息
     * @param arg 事务ID（订单ID）
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Long orderId = (Long) arg;
        
        try {
            // 查询订单是否存在且状态正确
            Order order = orderMapper.selectById(orderId);
            
            if (order != null && order.getStatus() == 0) { // 待支付状态
                log.info("[事务消息] 本地事务执行成功，orderId={}", orderId);
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                log.warn("[事务消息] 订单不存在或状态异常，orderId={}, order={}", orderId, order);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        } catch (Exception e) {
            log.error("[事务消息] 本地事务执行异常，orderId={}", orderId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务回查（网络异常时 MQ 主动询问）
     *
     * @param msg 消息
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Long orderId = msg.getHeaders().get("orderId", Long.class);
        
        if (orderId == null) {
            log.warn("[事务回查] 消息中缺少orderId");
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        
        try {
            Order order = orderMapper.selectById(orderId);
            
            if (order != null && order.getStatus() == 0) {
                log.info("[事务回查] 订单存在且状态正常，提交消息，orderId={}", orderId);
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                log.warn("[事务回查] 订单不存在或状态异常，回滚消息，orderId={}", orderId);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        } catch (Exception e) {
            log.error("[事务回查] 查询订单异常，orderId={}", orderId, e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}
