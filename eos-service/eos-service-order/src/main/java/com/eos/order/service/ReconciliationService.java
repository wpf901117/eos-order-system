package com.eos.order.service;

import com.eos.order.entity.Reconciliation;

import java.time.LocalDate;

/**
 * 对账服务接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
public interface ReconciliationService {

    /**
     * 执行对账任务
     *
     * @param date 对账日期
     * @return 对账记录
     */
    Reconciliation executeReconciliation(LocalDate date);

    /**
     * 查询对账记录
     *
     * @param reconciliationId 对账记录ID
     * @return 对账记录
     */
    Reconciliation getReconciliationById(Long reconciliationId);

    /**
     * 重新对账
     *
     * @param reconciliationId 对账记录ID
     * @return 对账记录
     */
    Reconciliation retryReconciliation(Long reconciliationId);
}
