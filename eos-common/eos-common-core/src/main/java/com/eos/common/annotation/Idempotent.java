package com.eos.common.annotation;

import java.lang.annotation.*;

/**
 * 幂等性注解
 *
 * <p>用于防止接口重复提交，基于 Redis 实现分布式幂等控制。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>订单创建</li>
 *   <li>支付请求</li>
 *   <li>表单提交</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @PostMapping("/create")
 * @Idempotent(key = "#dto.userId + ':' + #dto.productId", expire = 30)
 * public Result<OrderVO> createOrder(@RequestBody OrderCreateDTO dto) {
 *     return orderService.createOrder(dto);
 * }
 * }</pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键表达式（支持 SpEL）
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 过期时间（秒）
     *
     * @return 过期时间，默认60秒
     */
    long expire() default 60;

    /**
     * 错误提示信息
     *
     * @return 错误消息
     */
    String message() default "请勿重复提交";
}
