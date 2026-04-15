package com.eos.order.job;

import com.eos.order.service.ReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 对账定时任务
 *
 * <p>每日凌晨2点自动执行对账任务。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ReconciliationJob {

    @Autowired
    private ReconciliationService reconciliationService;

    /**
     * 每日凌晨2点执行对账
     *
     * Cron 表达式：秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天凌晨2点
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReconciliation() {
        log.info("[对账定时任务] 开始执行");

        try {
            // 对昨天的数据进行对账
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            reconciliationService.executeReconciliation(yesterday);
            
            log.info("[对账定时任务] 执行成功，日期: {}", yesterday);

        } catch (Exception e) {
            log.error("[对账定时任务] 执行失败", e);
        }
    }

    /**
     * 手动触发对账（用于测试或补跑）
     *
     * @param date 对账日期
     */
    public void manualReconciliation(LocalDate date) {
        log.info("[对账定时任务] 手动触发对账，日期: {}", date);
        
        try {
            reconciliationService.executeReconciliation(date);
            log.info("[对账定时任务] 手动对账成功");
        } catch (Exception e) {
            log.error("[对账定时任务] 手动对账失败", e);
            throw e;
        }
    }
}
