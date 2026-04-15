package com.eos.common.aspect;

import com.eos.common.annotation.RateLimit;
import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面
 *
 * <p>通过 AOP 拦截带有 {@link RateLimit} 注解的方法，
 * 基于 Redis + Lua 脚本实现原子性限流控制。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ExpressionParser parser = new SpelExpressionParser();

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Lua 脚本：原子性实现滑动窗口限流
     * KEYS[1]: 限流键
     * ARGV[1]: 时间窗口（秒）
     * ARGV[2]: 最大请求次数
     */
    private static final String LUA_SCRIPT = 
        "local key = KEYS[1]\n" +
        "local window = tonumber(ARGV[1])\n" +
        "local maxRequests = tonumber(ARGV[2])\n" +
        "local current = redis.call('GET', key)\n" +
        "if current and tonumber(current) >= maxRequests then\n" +
        "    return 0\n" +
        "end\n" +
        "current = redis.call('INCR', key)\n" +
        "if tonumber(current) == 1 then\n" +
        "    redis.call('EXPIRE', key, window)\n" +
        "end\n" +
        "return 1";

    private final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    /**
     * 环绕通知：处理限流逻辑
     *
     * @param joinPoint 连接点
     * @param rateLimit 限流注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 解析限流键
        String key = parseKey(rateLimit.key(), joinPoint);
        String rateLimitKey = RATE_LIMIT_PREFIX + key;

        // 2. 执行 Lua 脚本进行限流判断
        Long result = redisTemplate.execute(
            redisScript,
            Collections.singletonList(rateLimitKey),
            rateLimit.windowSeconds(),
            rateLimit.maxRequests()
        );

        if (result != null && result == 0) {
            log.warn("[限流] 请求被拦截，key={}, maxRequests={}, window={}s", 
                    rateLimitKey, rateLimit.maxRequests(), rateLimit.windowSeconds());
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, rateLimit.message());
        }

        log.debug("[限流] 请求通过，key={}", rateLimitKey);

        // 3. 执行业务逻辑
        return joinPoint.proceed();
    }

    /**
     * 解析 SpEL 表达式生成限流键
     *
     * @param keyExpression SpEL 表达式
     * @param joinPoint     连接点
     * @return 限流键
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);

        if (value == null) {
            throw new IllegalArgumentException("限流键不能为空: " + keyExpression);
        }

        return value.toString();
    }
}
