package com.eos.order.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 *
 * <p>用于标记需要记录审计日志的方法。</p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @AuditLog(module = "订单管理", operation = "创建订单")
 * public OrderVO createOrder(OrderCreateDTO dto) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 模块名称
     */
    String module();

    /**
     * 操作描述
     */
    String operation();

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResult() default false;
}
