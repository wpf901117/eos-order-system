package com.eos.order.seckill;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>Redis 预扣库存（Lua 脚本保证原子性）</li>
 *   <li>防重复提交</li>
 *   <li>限流控制</li>
 *   <li>异步下单</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class SeckillService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 秒杀商品库存 Key 前缀
     */
    private static final String SECKILL_STOCK_KEY = "seckill:stock:%d";

    /**
     * 秒杀用户购买记录 Key 前缀
     */
    private static final String SECKILL_USER_KEY = "seckill:user:%d:%d";

    /**
     * Lua 脚本：原子性扣减库存
     *
     * KEYS[1]: 库存 Key
     * ARGV[1]: 扣减数量
     * 返回值：剩余库存，-1 表示库存不足
     */
    private static final String LUA_DEDUCT_STOCK = 
        "local stock = tonumber(redis.call('GET', KEYS[1]))\n" +
        "if stock == nil then\n" +
        "   return -2\n" +  // 商品不存在
        "end\n" +
        "if stock < tonumber(ARGV[1]) then\n" +
        "   return -1\n" +  // 库存不足
        "end\n" +
        "redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
        "return redis.call('GET', KEYS[1])";

    /**
     * 初始化秒杀库存
     *
     * @param productId 商品ID
     * @param stock 库存数量
     */
    public void initSeckillStock(Long productId, Integer stock) {
        String key = String.format(SECKILL_STOCK_KEY, productId);
        redisTemplate.opsForValue().set(key, stock, 24, TimeUnit.HOURS);
        log.info("[秒杀系统] 初始化库存，productId={}, stock={}", productId, stock);
    }

    /**
     * 执行秒杀（原子性扣减库存）
     *
     * @param productId 商品ID
     * @param userId 用户ID
     * @param quantity 购买数量
     * @return 是否成功
     */
    public boolean executeSeckill(Long productId, Long userId, Integer quantity) {
        String stockKey = String.format(SECKILL_STOCK_KEY, productId);
        String userKey = String.format(SECKILL_USER_KEY, productId, userId);

        // 1. 防重复提交：检查用户是否已购买
        Boolean hasPurchased = redisTemplate.hasKey(userKey);
        if (Boolean.TRUE.equals(hasPurchased)) {
            log.warn("[秒杀系统] 用户已购买，userId={}, productId={}", userId, productId);
            return false;
        }

        // 2. Lua 脚本原子性扣减库存
        RScript script = redissonClient.getScript();
        Object result = script.eval(
            RScript.Mode.READ_WRITE,
            LUA_DEDUCT_STOCK,
            RScript.ReturnType.INTEGER,
            Collections.singletonList(stockKey),
            quantity
        );

        int remainingStock = (Integer) result;

        if (remainingStock == -2) {
            log.warn("[秒杀系统] 商品不存在，productId={}", productId);
            return false;
        }

        if (remainingStock == -1) {
            log.warn("[秒杀系统] 库存不足，productId={}, userId={}", productId, userId);
            return false;
        }

        // 3. 记录用户购买信息（5分钟过期，防止重复购买）
        redisTemplate.opsForValue().set(userKey, System.currentTimeMillis(), 5, TimeUnit.MINUTES);

        log.info("[秒杀系统] 秒杀成功，productId={}, userId={}, 剩余库存={}", 
                 productId, userId, remainingStock);

        return true;
    }

    /**
     * 获取秒杀库存
     *
     * @param productId 商品ID
     * @return 剩余库存
     */
    public Integer getSeckillStock(Long productId) {
        String key = String.format(SECKILL_STOCK_KEY, productId);
        Object stock = redisTemplate.opsForValue().get(key);
        return stock != null ? (Integer) stock : 0;
    }

    /**
     * 回滚库存（秒杀失败时调用）
     *
     * @param productId 商品ID
     * @param quantity 回滚数量
     */
    public void rollbackStock(Long productId, Integer quantity) {
        String key = String.format(SECKILL_STOCK_KEY, productId);
        redisTemplate.opsForValue().increment(key, quantity);
        log.info("[秒杀系统] 库存回滚，productId={}, quantity={}", productId, quantity);
    }

    /**
     * 限流检查（令牌桶简化版）
     *
     * @param productId 商品ID
     * @param maxQps 最大 QPS
     * @return 是否允许通过
     */
    public boolean rateLimitCheck(Long productId, int maxQps) {
        String key = "seckill:rate_limit:" + productId;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            // 第一次访问，设置过期时间 1 秒
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }
        
        if (count > maxQps) {
            log.warn("[秒杀系统] 触发限流，productId={}, 当前QPS={}", productId, count);
            return false;
        }
        
        return true;
    }
}
