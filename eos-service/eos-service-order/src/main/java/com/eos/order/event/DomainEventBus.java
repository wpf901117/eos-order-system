package com.eos.order.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 领域事件总线
 *
 * <p>负责发布领域事件，支持同步和异步两种方式。</p>
 *
 * <p><strong>使用方式：</strong></p>
 * <pre>{@code
 * @Autowired
 * private DomainEventBus eventBus;
 * 
 * // 发布事件
 * eventBus.publish(new OrderCreatedEvent(...));
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class DomainEventBus {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 同步发布事件
     *
     * @param event 领域事件
     */
    public void publish(DomainEvent event) {
        log.debug("[事件总线] 发布事件: {}, aggregateId: {}", 
                  event.getEventType(), event.getAggregateId());
        eventPublisher.publishEvent(event);
    }

    /**
     * 异步发布事件
     *
     * @param event 领域事件
     */
    @Async("domainEventExecutor")
    public void publishAsync(DomainEvent event) {
        log.debug("[事件总线] 异步发布事件: {}, aggregateId: {}", 
                  event.getEventType(), event.getAggregateId());
        eventPublisher.publishEvent(event);
    }
}
