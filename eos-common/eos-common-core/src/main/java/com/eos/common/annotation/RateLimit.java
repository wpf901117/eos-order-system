package com.eos.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * <p>基于 Redis + Lua 脚本实现简单限流，生产环境建议使用 Sentinel。</p>
 *
 * <p><strong>限流算法：</strong>滑动窗口计数</p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @PostMapping("/create")
 * @RateLimit(key = "#userId", maxRequests = 100, windowSeconds = 60)
 * public Result<OrderVO> createOrder(@RequestParam Long userId) {
 *     return orderService.createOrder(userId);
 * }
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键（支持 SpEL 表达式）
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 最大请求次数
     *
     * @return 请求次数限制
     */
    int maxRequests() default 100;

    /**
     * 时间窗口（秒）
     *
     * @return 时间窗口大小
     */
    int windowSeconds() default 60;

    /**
     * 错误提示信息
     *
     * @return 错误消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
