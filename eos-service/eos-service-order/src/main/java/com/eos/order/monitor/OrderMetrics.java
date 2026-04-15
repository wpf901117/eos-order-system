package com.eos.order.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单业务监控指标
 *
 * <p>使用 Micrometer 记录关键业务指标，供 Prometheus 采集。</p>
 *
 * <p><strong>监控指标：</strong></p>
 * <ul>
 *   <li>订单创建总数（Counter）</li>
 *   <li>订单支付成功数（Counter）</li>
 *   <li>订单取消数（Counter）</li>
 *   <li>订单创建耗时（Timer）</li>
 *   <li>当前待支付订单数（Gauge）</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    // 计数器
    private Counter orderCreateCounter;
    private Counter orderPaySuccessCounter;
    private Counter orderCancelCounter;
    private Counter orderTimeoutCounter;

    // 计时器
    private Timer orderCreateTimer;

    // 仪表盘（实时值）
    private final AtomicLong pendingOrderCount = new AtomicLong(0);

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // 初始化计数器
        orderCreateCounter = Counter.builder("order.create.total")
                .description("订单创建总数")
                .tag("service", "order-service")
                .register(meterRegistry);

        orderPaySuccessCounter = Counter.builder("order.pay.success.total")
                .description("订单支付成功数")
                .tag("service", "order-service")
                .register(meterRegistry);

        orderCancelCounter = Counter.builder("order.cancel.total")
                .description("订单取消数")
                .tag("service", "order-service")
                .register(meterRegistry);

        orderTimeoutCounter = Counter.builder("order.timeout.total")
                .description("订单超时取消数")
                .tag("service", "order-service")
                .register(meterRegistry);

        // 初始化计时器
        orderCreateTimer = Timer.builder("order.create.duration")
                .description("订单创建耗时")
                .tag("service", "order-service")
                .register(meterRegistry);

        // 初始化仪表盘
        Gauge.builder("order.pending.count", pendingOrderCount, AtomicLong::get)
                .description("当前待支付订单数")
                .tag("service", "order-service")
                .register(meterRegistry);

        log.info("[监控] 订单业务指标初始化完成");
    }

    /**
     * 记录订单创建
     */
    public void recordOrderCreate() {
        orderCreateCounter.increment();
        pendingOrderCount.incrementAndGet();
        log.debug("[监控] 订单创建 +1，当前待支付: {}", pendingOrderCount.get());
    }

    /**
     * 记录订单支付成功
     */
    public void recordOrderPaySuccess() {
        orderPaySuccessCounter.increment();
        pendingOrderCount.decrementAndGet();
        log.debug("[监控] 订单支付成功 +1，当前待支付: {}", pendingOrderCount.get());
    }

    /**
     * 记录订单取消
     */
    public void recordOrderCancel() {
        orderCancelCounter.increment();
        pendingOrderCount.decrementAndGet();
        log.debug("[监控] 订单取消 +1，当前待支付: {}", pendingOrderCount.get());
    }

    /**
     * 记录订单超时
     */
    public void recordOrderTimeout() {
        orderTimeoutCounter.increment();
        pendingOrderCount.decrementAndGet();
        log.debug("[监控] 订单超时 +1，当前待支付: {}", pendingOrderCount.get());
    }

    /**
     * 记录订单创建耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordOrderCreateDuration(long durationMs) {
        orderCreateTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("[监控] 订单创建耗时: {}ms", durationMs);
    }

    /**
     * 获取当前待支付订单数
     *
     * @return 待支付订单数
     */
    public long getPendingOrderCount() {
        return pendingOrderCount.get();
    }
}
