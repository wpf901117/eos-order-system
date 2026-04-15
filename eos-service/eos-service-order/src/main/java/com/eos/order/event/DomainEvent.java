package com.eos.order.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 *
 * <p>所有领域事件都应继承此类，保证事件的统一性。</p>
 *
 * <p><strong>事件特性：</strong></p>
 * <ul>
 *   <li>不可变性：事件一旦发生，不可修改</li>
 *   <li>唯一标识：每个事件都有唯一的 eventId</li>
 *   <li>时间戳：记录事件发生的时间</li>
 *   <li>聚合根ID：标识事件所属的聚合根</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public abstract class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件唯一标识 */
    private final String eventId;

    /** 事件发生时间 */
    private final LocalDateTime occurredOn;

    /** 聚合根ID */
    private final Long aggregateId;

    /**
     * 构造函数
     *
     * @param aggregateId 聚合根ID
     */
    protected DomainEvent(Long aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型名称
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
