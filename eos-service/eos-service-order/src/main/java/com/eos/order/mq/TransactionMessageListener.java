package com.eos.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMQ 事务消息监听器
 *
 * <p>实现最终一致性：本地事务 + MQ 事务消息。</p>
 *
 * <p><strong>执行流程：</strong></p>
 * <ol>
 *   <li>发送 Half 消息（对消费者不可见）</li>
 *   <li>执行本地事务</li>
 *   <li>根据本地事务结果提交/回滚 Half 消息</li>
 *   <li>如果未收到响应，MQ 会回调 checkLocalTransaction</li>
 * </ol>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class TransactionMessageListener implements TransactionListener {

    /**
     * 本地事务状态缓存
     * Key: transactionId
     * Value: LocalTransactionState
     */
    private final ConcurrentHashMap<String, LocalTransactionState> localTransactionMap = new ConcurrentHashMap<>();

    /**
     * 执行本地事务
     *
     * @param message Half 消息
     * @param arg 业务参数
     * @return 本地事务状态
     */
    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
        String transactionId = message.getTransactionId();
        log.info("[事务消息] 开始执行本地事务，transactionId={}", transactionId);

        try {
            // 从参数中获取业务数据
            String businessData = (String) arg;
            
            // TODO: 执行本地事务（如创建订单、扣减库存等）
            // orderService.createOrder(...);
            
            log.info("[事务消息] 本地事务执行成功，transactionId={}", transactionId);
            
            // 记录事务状态
            localTransactionMap.put(transactionId, LocalTransactionState.COMMIT_MESSAGE);
            
            return LocalTransactionState.COMMIT_MESSAGE;

        } catch (Exception e) {
            log.error("[事务消息] 本地事务执行失败，transactionId={}", transactionId, e);
            
            // 记录事务状态
            localTransactionMap.put(transactionId, LocalTransactionState.ROLLBACK_MESSAGE);
            
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    /**
     * 检查本地事务状态（MQ 回调）
     *
     * <p>当 MQ 未收到本地事务的提交/回滚响应时，会定期回调此方法。</p>
     *
     * @param messageExt Half 消息
     * @return 本地事务状态
     */
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
        String transactionId = messageExt.getTransactionId();
        log.info("[事务消息] 检查本地事务状态，transactionId={}", transactionId);

        LocalTransactionState state = localTransactionMap.get(transactionId);
        
        if (state != null) {
            log.info("[事务消息] 找到本地事务状态，transactionId={}, state={}", transactionId, state);
            return state;
        }

        // 如果找不到状态，可能是服务重启导致内存数据丢失
        // 实际生产环境应该查询数据库或 Redis 确认事务状态
        log.warn("[事务消息] 未找到本地事务状态，需要查询数据库确认，transactionId={}", transactionId);
        
        // TODO: 查询数据库确认事务是否成功
        // boolean success = orderMapper.existsByTransactionId(transactionId);
        // return success ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
        
        // 暂时返回未知状态，等待下次检查
        return LocalTransactionState.UNKNOW;
    }
}
