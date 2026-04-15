package com.eos.common.aspect;

import com.eos.common.annotation.Idempotent;
import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性切面
 *
 * <p>通过 AOP 拦截带有 {@link Idempotent} 注解的方法，
 * 基于 Redis SET NX 实现分布式幂等控制。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 环绕通知：处理幂等性逻辑
     *
     * @param joinPoint  连接点
     * @param idempotent 幂等注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 解析幂等键
        String key = parseKey(idempotent.key(), joinPoint);
        String idempotentKey = IDEMPOTENT_PREFIX + key;

        // 2. 尝试设置幂等键（SET NX）
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", idempotent.expire(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            log.warn("[幂等性] 重复请求被拦截，key={}, message={}", idempotentKey, idempotent.message());
            throw new BizException(ResultCode.DUPLICATE_REQUEST, idempotent.message());
        }

        log.debug("[幂等性] 请求通过，key={}", idempotentKey);

        try {
            // 3. 执行业务逻辑
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 4. 业务异常时删除幂等键，允许重试
            redisTemplate.delete(idempotentKey);
            log.warn("[幂等性] 业务异常，删除幂等键，key={}", idempotentKey);
            throw e;
        }
    }

    /**
     * 解析 SpEL 表达式生成幂等键
     *
     * @param keyExpression SpEL 表达式
     * @param joinPoint     连接点
     * @return 幂等键
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 创建 SpEL 上下文
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 解析表达式
        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);

        if (value == null) {
            throw new IllegalArgumentException("幂等键不能为空: " + keyExpression);
        }

        return value.toString();
    }
}
