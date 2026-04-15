package com.eos.common.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置类
 *
 * <p>Spring Boot 自动配置的 RedisTemplate 存在以下问题：</p>
 * <ul>
 *   <li>默认使用 JdkSerializationRedisSerializer，序列化后的数据不可读</li>
 *   <li>Key 和 Value 都使用二进制序列化，不便于 Redis 命令行调试</li>
 * </ul>
 *
 * <p>本配置类解决以上问题，采用 JSON 序列化方案，并启用 Spring Cache 注解支持。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@EnableCaching
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate
     *
     * <p>配置要点：</p>
     * <ul>
     *   <li>Key 采用 String 序列化（便于人眼识别和命令行操作）</li>
     *   <li>Value 采用 JSON 序列化（跨语言、可读性好）</li>
     *   <li>Hash 的 key/value 也采用同样的策略</li>
     * </ul>
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化器：String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化器：JSON（带类型信息，保证反序列化正确）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Spring Cache 管理器
     *
     * <p>使用 @Cacheable、@CacheEvict、@CachePut 注解时，
     * 会通过此管理器操作 Redis 缓存。</p>
     *
     * @param factory Redis 连接工厂
     * @return CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 默认过期时间 30 分钟
                .entryTtl(Duration.ofMinutes(30))
                // 禁止缓存 null 值
                .disableCachingNullValues()
                // Key 序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper())));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * 配置 ObjectMapper，为 JSON 序列化添加类型信息
     *
     * <p>如果不添加类型信息，反序列化时无法知道原始类型，会变成 LinkedHashMap。
     * 通过 {@code @class} 字段保存完整类名，确保复杂对象能正确还原。</p>
     *
     * @return 配置后的 ObjectMapper
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
