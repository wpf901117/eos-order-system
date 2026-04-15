package com.eos.order.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 布隆过滤器服务
 *
 * <p>用于防止缓存穿透，在查询缓存前先判断 key 是否存在。</p>
 *
 * <p><strong>工作原理：</strong></p>
 * <ol>
 *   <li>数据写入时，同时将 key 加入布隆过滤器</li>
 *   <li>查询时，先检查布隆过滤器</li>
 *   <li>如果布隆过滤器返回 false，说明 key 一定不存在，直接返回</li>
 *   <li>如果返回 true，说明 key 可能存在，继续查询缓存</li>
 * </ol>
 *
 * <p><strong>误判率：</strong>1%（可配置）</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class BloomFilterService {

    @Autowired
    private RedissonClient redissonClient;

    /** 商品布隆过滤器 */
    private RBloomFilter<Long> productBloomFilter;

    /** 订单布隆过滤器 */
    private RBloomFilter<Long> orderBloomFilter;

    /** 预期元素数量 */
    private static final long EXPECTED_INSERTIONS = 1000000L;

    /** 误判率 */
    private static final double FALSE_PROBABILITY = 0.01;

    @PostConstruct
    public void init() {
        // 初始化商品布隆过滤器
        productBloomFilter = redissonClient.getBloomFilter("product:bloom");
        if (!productBloomFilter.isExists()) {
            productBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            log.info("[布隆过滤器] 商品布隆过滤器初始化完成，预期元素: {}, 误判率: {}", 
                     EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        }

        // 初始化订单布隆过滤器
        orderBloomFilter = redissonClient.getBloomFilter("order:bloom");
        if (!orderBloomFilter.isExists()) {
            orderBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            log.info("[布隆过滤器] 订单布隆过滤器初始化完成，预期元素: {}, 误判率: {}", 
                     EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        }
    }

    /**
     * 添加商品 ID 到布隆过滤器
     *
     * @param productId 商品ID
     */
    public void addProduct(Long productId) {
        if (productId != null) {
            productBloomFilter.add(productId);
            log.debug("[布隆过滤器] 添加商品ID: {}", productId);
        }
    }

    /**
     * 添加订单 ID 到布隆过滤器
     *
     * @param orderId 订单ID
     */
    public void addOrder(Long orderId) {
        if (orderId != null) {
            orderBloomFilter.add(orderId);
            log.debug("[布隆过滤器] 添加订单ID: {}", orderId);
        }
    }

    /**
     * 判断商品 ID 是否可能存在
     *
     * @param productId 商品ID
     * @return true 表示可能存在，false 表示一定不存在
     */
    public boolean mightContainProduct(Long productId) {
        if (productId == null) {
            return false;
        }
        boolean exists = productBloomFilter.contains(productId);
        log.debug("[布隆过滤器] 检查商品ID: {}, 结果: {}", productId, exists);
        return exists;
    }

    /**
     * 判断订单 ID 是否可能存在
     *
     * @param orderId 订单ID
     * @return true 表示可能存在，false 表示一定不存在
     */
    public boolean mightContainOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        boolean exists = orderBloomFilter.contains(orderId);
        log.debug("[布隆过滤器] 检查订单ID: {}, 结果: {}", orderId, exists);
        return exists;
    }

    /**
     * 获取布隆过滤器统计信息
     *
     * @return 统计信息
     */
    public String getStats() {
        return String.format(
            "商品布隆过滤器 - 元素数量: %d, 预期容量: %d, 误判率: %.2f%%\n" +
            "订单布隆过滤器 - 元素数量: %d, 预期容量: %d, 误判率: %.2f%%",
            productBloomFilter.count(),
            EXPECTED_INSERTIONS,
            FALSE_PROBABILITY * 100,
            orderBloomFilter.count(),
            EXPECTED_INSERTIONS,
            FALSE_PROBABILITY * 100
        );
    }
}
