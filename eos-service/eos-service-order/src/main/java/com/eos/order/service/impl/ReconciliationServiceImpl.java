package com.eos.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eos.order.entity.Order;
import com.eos.order.entity.Reconciliation;
import com.eos.order.entity.ReconciliationDiff;
import com.eos.order.mapper.OrderMapper;
import com.eos.order.mapper.ReconciliationDiffMapper;
import com.eos.order.mapper.ReconciliationMapper;
import com.eos.order.service.ReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对账服务实现类
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>定时对账：每日凌晨2点自动执行</li>
 *   <li>订单与支付流水比对</li>
 *   <li>差异检测与记录</li>
 *   <li>对账报告生成</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class ReconciliationServiceImpl implements ReconciliationService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReconciliationMapper reconciliationMapper;

    @Autowired
    private ReconciliationDiffMapper diffMapper;

    /**
     * 执行对账任务
     *
     * @param date 对账日期
     * @return 对账记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reconciliation executeReconciliation(LocalDate date) {
        log.info("[对账系统] 开始对账，日期: {}", date);

        // 1. 创建对账记录
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setReconciliationDate(date);
        reconciliation.setStatus(1); // 对账中
        reconciliation.setStartTime(LocalDateTime.now());
        reconciliationMapper.insert(reconciliation);

        try {
            // 2. 查询当日订单数据
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, startOfDay)
                    .lt(Order::getCreateTime, endOfDay)
                    .eq(Order::getStatus, 1) // 已支付订单
            );

            // 3. 统计订单数据
            int totalOrderCount = orders.size();
            BigDecimal totalOrderAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            reconciliation.setTotalOrderCount(totalOrderCount);
            reconciliation.setTotalOrderAmount(totalOrderAmount);

            // 4. 模拟查询支付流水（实际应调用支付服务）
            Map<String, BigDecimal> paymentMap = queryPaymentRecords(date);
            int totalPaymentCount = paymentMap.size();
            BigDecimal totalPaymentAmount = paymentMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            reconciliation.setTotalPaymentCount(totalPaymentCount);
            reconciliation.setTotalPaymentAmount(totalPaymentAmount);

            // 5. 比对差异
            List<ReconciliationDiff> diffs = compareOrdersAndPayments(orders, paymentMap, reconciliation.getId());
            int diffCount = diffs.size();
            BigDecimal diffAmount = diffs.stream()
                .map(ReconciliationDiff::getDiffAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            reconciliation.setDiffCount(diffCount);
            reconciliation.setDiffAmount(diffAmount);

            // 6. 保存差异明细
            if (!diffs.isEmpty()) {
                diffs.forEach(diffMapper::insert);
            }

            // 7. 更新对账状态
            reconciliation.setStatus(diffCount == 0 ? 2 : 3); // 2-成功，3-失败
            reconciliation.setEndTime(LocalDateTime.now());
            reconciliationMapper.updateById(reconciliation);

            log.info("[对账系统] 对账完成，日期: {}, 订单数: {}, 支付数: {}, 差异数: {}",
                    date, totalOrderCount, totalPaymentCount, diffCount);

            return reconciliation;

        } catch (Exception e) {
            log.error("[对账系统] 对账失败，日期: {}", date, e);

            // 更新失败状态
            reconciliation.setStatus(3);
            reconciliation.setErrorMsg(e.getMessage());
            reconciliation.setEndTime(LocalDateTime.now());
            reconciliationMapper.updateById(reconciliation);

            throw e;
        }
    }

    @Override
    public Reconciliation getReconciliationById(Long reconciliationId) {
        return reconciliationMapper.selectById(reconciliationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reconciliation retryReconciliation(Long reconciliationId) {
        Reconciliation reconciliation = reconciliationMapper.selectById(reconciliationId);
        if (reconciliation == null) {
            throw new IllegalArgumentException("对账记录不存在");
        }

        // 删除旧的对账差异
        diffMapper.delete(new LambdaQueryWrapper<ReconciliationDiff>()
            .eq(ReconciliationDiff::getReconciliationId, reconciliationId));

        // 重新执行对账
        return executeReconciliation(reconciliation.getReconciliationDate());
    }

    /**
     * 查询支付流水记录（模拟）
     *
     * @param date 对账日期
     * @return 订单号 -> 支付金额
     */
    private Map<String, BigDecimal> queryPaymentRecords(LocalDate date) {
        // TODO: 实际应调用支付服务接口或查询支付流水表
        // 这里返回空 Map 作为示例
        log.warn("[对账系统] 支付流水查询未实现，返回空数据");
        return new HashMap<>();
    }

    /**
     * 比对订单和支付流水
     *
     * @param orders 订单列表
     * @param paymentMap 支付流水映射
     * @param reconciliationId 对账记录ID
     * @return 差异列表
     */
    private List<ReconciliationDiff> compareOrdersAndPayments(
            List<Order> orders,
            Map<String, BigDecimal> paymentMap,
            Long reconciliationId) {

        List<ReconciliationDiff> diffs = new ArrayList<>();

        // 构建订单映射
        Map<String, Order> orderMap = orders.stream()
            .collect(Collectors.toMap(Order::getOrderNo, o -> o));

        // 1. 检查订单有但支付无的情况
        for (Order order : orders) {
            if (!paymentMap.containsKey(order.getOrderNo())) {
                ReconciliationDiff diff = new ReconciliationDiff();
                diff.setReconciliationId(reconciliationId);
                diff.setOrderNo(order.getOrderNo());
                diff.setOrderAmount(order.getTotalAmount());
                diff.setDiffType(1); // 订单有支付无
                diff.setDiffAmount(order.getTotalAmount());
                diff.setHandleStatus(0);
                diffs.add(diff);

                log.warn("[对账系统] 发现差异：订单有支付无，orderNo={}", order.getOrderNo());
            }
        }

        // 2. 检查支付有但订单无的情况
        for (Map.Entry<String, BigDecimal> entry : paymentMap.entrySet()) {
            String orderNo = entry.getKey();
            BigDecimal paymentAmount = entry.getValue();

            if (!orderMap.containsKey(orderNo)) {
                ReconciliationDiff diff = new ReconciliationDiff();
                diff.setReconciliationId(reconciliationId);
                diff.setOrderNo(orderNo);
                diff.setPaymentNo(orderNo);
                diff.setPaymentAmount(paymentAmount);
                diff.setDiffType(2); // 支付有订单无
                diff.setDiffAmount(paymentAmount);
                diff.setHandleStatus(0);
                diffs.add(diff);

                log.warn("[对账系统] 发现差异：支付有订单无，orderNo={}", orderNo);
            }
        }

        // 3. 检查金额不一致的情况
        for (Order order : orders) {
            BigDecimal paymentAmount = paymentMap.get(order.getOrderNo());
            if (paymentAmount != null && order.getTotalAmount().compareTo(paymentAmount) != 0) {
                ReconciliationDiff diff = new ReconciliationDiff();
                diff.setReconciliationId(reconciliationId);
                diff.setOrderNo(order.getOrderNo());
                diff.setOrderAmount(order.getTotalAmount());
                diff.setPaymentNo(order.getOrderNo());
                diff.setPaymentAmount(paymentAmount);
                diff.setDiffType(3); // 金额不一致
                diff.setDiffAmount(order.getTotalAmount().subtract(paymentAmount).abs());
                diff.setHandleStatus(0);
                diffs.add(diff);

                log.warn("[对账系统] 发现差异：金额不一致，orderNo={}, 订单金额={}, 支付金额={}",
                        order.getOrderNo(), order.getTotalAmount(), paymentAmount);
            }
        }

        return diffs;
    }
}
