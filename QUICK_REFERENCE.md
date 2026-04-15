# EOS 项目 - 快速参考手册

> 快速查找代码位置、实现方式和使用示例

---

## 📍 核心功能速查表

### 1. 认证与授权

| 功能 | 文件路径 | 关键类/方法 |
|------|---------|------------|
| JWT 工具类 | `eos-common/eos-common-core/src/main/java/com/eos/common/util/JwtUtil.java` | `generateAccessToken()`, `validateToken()` |
| 网关认证过滤器 | `eos-gateway/src/main/java/com/eos/gateway/filter/AuthFilter.java` | `filter()` |
| 用户登录 | `eos-service/eos-service-user/src/main/java/com/eos/user/service/impl/UserServiceImpl.java` | `login()` |
| Token 刷新 | `eos-service/eos-service-user/src/main/java/com/eos/user/service/impl/UserServiceImpl.java` | `refresh()` |
| 用户登出 | `eos-service/eos-service-user/src/main/java/com/eos/user/service/impl/UserServiceImpl.java` | `logout()` |

**使用示例**：

```java
// 前端请求头
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

// 网关自动解析并透传
X-User-Id: 1000000000000000002
X-Username: zhangsan
X-Role: USER
```

---

### 2. 高并发防护

| 功能 | 文件路径 | 注解/工具 |
|------|---------|----------|
| 幂等性注解 | `eos-common/eos-common-core/src/main/java/com/eos/common/annotation/Idempotent.java` | `@Idempotent` |
| 幂等性切面 | `eos-common/eos-common-core/src/main/java/com/eos/common/aspect/IdempotentAspect.java` | AOP |
| 限流注解 | `eos-common/eos-common-core/src/main/java/com/eos/common/annotation/RateLimit.java` | `@RateLimit` |
| 限流切面 | `eos-common/eos-common-core/src/main/java/com/eos/common/aspect/RateLimitAspect.java` | AOP + Lua |
| 分布式锁 | `eos-common/eos-common-core/src/main/java/com/eos/common/util/DistributedLockUtil.java` | `DistributedLockUtil.executeWithLock()` |

**使用示例**：

```java
// 幂等性
@Idempotent(key = "#userId + ':' + #dto.productId", expire = 30)
@PostMapping("/create")
public Result<OrderVO> createOrder(...) { }

// 限流
@RateLimit(key = "#userId", maxRequests = 10, windowSeconds = 60)
@PostMapping("/create")
public Result<OrderVO> createOrder(...) { }

// 分布式锁
DistributedLockUtil.executeWithLock(redissonClient, lockKey, () -> {
    // 业务逻辑
});
```

---

### 3. 分布式事务

| 方案 | 文件路径 | 关键配置 |
|------|---------|---------|
| Seata AT | `eos-service/eos-service-order/src/main/resources/application-seata.yml` | `@GlobalTransactional` |
| RocketMQ 事务消息 | `eos-service/eos-service-order/src/main/java/com/eos/order/mq/OrderCreatedTransactionListener.java` | `@RocketMQTransactionListener` |

**使用示例**：

```java
// Seata AT 模式
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
    // 1. 扣减库存（远程）
    inventoryFeignClient.deductStock(...);
    
    // 2. 创建订单（本地）
    orderMapper.insert(order);
    
    // 3. 扣减余额（远程）
    accountFeignClient.deductBalance(...);
}

// RocketMQ 事务消息
@RocketMQTransactionListener
public class OrderCreatedTransactionListener implements RocketMQLocalTransactionListener {
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 执行本地事务
        return RocketMQLocalTransactionState.COMMIT;
    }
    
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 事务回查
        return RocketMQLocalTransactionState.COMMIT;
    }
}
```

---

### 4. DDD 领域模型

| 概念 | 文件路径 | 说明 |
|------|---------|------|
| 聚合根 | `eos-service/eos-service-order/src/main/java/com/eos/order/domain/Order.java` | Order 充血模型 |
| 值对象 | `eos-service/eos-service-order/src/main/java/com/eos/order/domain/Money.java` | Money 不可变对象 |
| 枚举 | `eos-service/eos-service-order/src/main/java/com/eos/order/domain/OrderStatus.java` | 订单状态 |
| 应用服务 | `eos-service/eos-service-order/src/main/java/com/eos/order/application/OrderApplicationService.java` | 编排领域对象 |

**使用示例**：

```java
// 创建订单（工厂方法）
Money price = new Money(new BigDecimal("100"));
Order order = Order.create(userId, productId, productName, quantity, price, address);

// 调用领域行为
order.pay();      // 支付
order.cancel();   // 取消
order.ship();     // 发货
order.confirm();  // 确认收货
```

---

### 5. JDK 21 新特性

| 特性 | 文件路径 | 示例 |
|------|---------|------|
| 虚拟线程 | `eos-service/eos-service-order/src/main/java/com/eos/order/config/VirtualThreadConfig.java` | `Executors.newVirtualThreadPerTaskExecutor()` |
| Record | `eos-service/eos-service-order/src/main/java/com/eos/order/dto/OrderStatisticsDTO.java` | `public record OrderStatisticsDTO(...)` |
| Pattern Matching | `eos-service/eos-service-order/src/main/java/com/eos/order/service/OrderStatisticsService.java` | `switch (obj) { case Order o -> ... }` |

**使用示例**：

```java
// 虚拟线程
@Bean("virtualThreadExecutor")
public ExecutorService virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

// Record
public record OrderStatisticsDTO(
    Long userId, Integer totalOrders, 
    BigDecimal totalAmount, 
    Integer paidOrders, Integer unpaidOrders
) {}

// Pattern Matching
return switch (status) {
    case 0 -> "待支付";
    case 1 -> "已支付";
    case null, default -> "未知";
};
```

---

### 6. 弹性设计（Sentinel）

| 功能 | 文件路径 | 关键类 |
|------|---------|--------|
| Sentinel 配置 | `eos-service/eos-service-order/src/main/java/com/eos/order/config/SentinelConfig.java` | `SentinelConfig` |
| 熔断降级示例 | `eos-service/eos-service-order/src/main/java/com/eos/order/service/ResilientOrderService.java` | `@SentinelResource` |

**使用示例**：

```java
@SentinelResource(
    value = "getProductById",
    blockHandler = "handleBlockException",
    fallback = "handleFallback"
)
public ProductVO getProductWithFallback(Long productId) {
    return productFeignClient.getProductById(productId).getData();
}

// 流控处理
public ProductVO handleBlockException(Long productId, BlockException ex) {
    throw new BizException(ResultCode.TOO_MANY_REQUESTS);
}

// 降级处理
public ProductVO handleFallback(Long productId, Throwable t) {
    return getDefaultProduct(productId);  // 返回缓存或默认值
}
```

---

### 7. 可观测性与监控

| 功能 | 文件路径 | 关键类 |
|------|---------|--------|
| Actuator 配置 | `eos-service/eos-service-order/src/main/resources/application.yml` | `management.*` |
| 自定义指标 | `eos-service/eos-service-order/src/main/java/com/eos/order/monitor/OrderMetrics.java` | `OrderMetrics` |

**访问监控端点**：

```bash
# 健康检查
curl http://localhost:8083/actuator/health

# 应用信息
curl http://localhost:8083/actuator/info

# Prometheus 指标
curl http://localhost:8083/actuator/prometheus
```

**自定义指标使用**：

```java
@Autowired
private OrderMetrics orderMetrics;

// 记录订单创建
orderMetrics.recordOrderCreate();
orderMetrics.recordOrderCreateDuration(durationMs);

// 记录订单支付
orderMetrics.recordOrderPaySuccess();

// 记录订单取消
orderMetrics.recordOrderCancel();
orderMetrics.recordOrderTimeout();
```

**Prometheus 查询**：

```promql
# 订单创建速率（QPS）
rate(order_create_total[5m])

# 订单创建平均耗时
rate(order_create_duration_sum[5m]) / rate(order_create_duration_count[5m])

# 待支付订单数
order_pending_count
```

---

## 🔧 配置文件速查

### 数据库配置

**位置**：各服务的 `application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/eos_order?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### Redis 配置

**位置**：各服务的 `application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### Nacos 配置

**位置**：各服务的 `application.yml`

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
```

### Seata 配置

**位置**：`eos-service/eos-service-order/src/main/resources/application-seata.yml`

```yaml
seata:
  enabled: true
  tx-service-group: eos_tx_group
  config:
    type: nacos
  registry:
    type: nacos
```

---

## 📦 Maven 依赖速查

### 父 POM 版本管理

**位置**：`pom.xml`

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.5</spring-boot.version>
    <spring-cloud.version>2023.0.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
</properties>
```

### 常用依赖

```xml
<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MyBatis Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Redisson -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>

<!-- Sentinel -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

<!-- Seata -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>

<!-- RocketMQ -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

---

## 🎯 常见场景实现速查

### 场景1：防止重复提交

```java
// Controller 层
@Idempotent(key = "#userId + ':' + #dto.productId", expire = 30)
@PostMapping("/create")
public Result<OrderVO> createOrder(...) { }

// Service 层
DistributedLockUtil.executeWithLock(redissonClient, lockKey, () -> {
    // 业务逻辑
});
```

### 场景2：限流保护

```java
@RateLimit(key = "#userId", maxRequests = 10, windowSeconds = 60)
@PostMapping("/create")
public Result<OrderVO> createOrder(...) { }
```

### 场景3：跨服务调用

```java
@FeignClient(name = "eos-product-service")
public interface ProductFeignClient {
    @GetMapping("/product/{productId}")
    Result<ProductVO> getProductById(@PathVariable Long productId);
}
```

### 场景4：异步任务

```java
@Autowired
@Qualifier("virtualThreadExecutor")
private ExecutorService virtualThreadExecutor;

public void sendNotificationAsync(Long orderId) {
    virtualThreadExecutor.submit(() -> {
        // 异步发送通知
    });
}
```

### 场景5：缓存查询

```java
public ProductVO getProduct(Long productId) {
    // L1: 本地缓存
    ProductVO product = localCache.getIfPresent(productId);
    if (product != null) return product;
    
    // L2: Redis
    product = redisTemplate.opsForValue().get("product:" + productId);
    if (product != null) {
        localCache.put(productId, product);
        return product;
    }
    
    // L3: 数据库
    product = productMapper.selectById(productId);
    if (product != null) {
        redisTemplate.opsForValue().set("product:" + productId, product, 1, TimeUnit.HOURS);
        localCache.put(productId, product);
    }
    
    return product;
}
```

---

## 🐛 常见问题排查

### 问题1：Token 验证失败

**检查项**：
1. 请求头是否包含 `Authorization: Bearer xxx`
2. Token 是否过期
3. Session 是否被撤销（Redis 中是否存在）

**日志关键字**：
```
[网关认证] Token无效或已过期
[网关认证] Session已撤销
```

### 问题2：分布式锁获取失败

**检查项**：
1. Redis 连接是否正常
2. 锁的 key 是否正确
3. 是否有死锁

**日志关键字**：
```
[分布式锁] 获取锁失败
[分布式锁] 释放锁异常
```

### 问题3：分布式事务回滚

**检查项**：
1. Seata Server 是否启动
2. 全局事务 ID 是否正确传递
3. 参与方是否都注册到 Seata

**日志关键字**：
```
[Seata] 全局事务开始
[Seata] 全局事务回滚
```

---

## 📊 性能优化建议

### 数据库优化

```sql
-- 添加索引
CREATE INDEX idx_user_status_time ON t_order(user_id, status, create_time);

-- 避免 SELECT *
SELECT id, order_no, amount FROM orders WHERE user_id = ?;

-- 分页优化
SELECT * FROM orders WHERE id > last_id LIMIT 10;
```

### 缓存优化

```java
// 设置合理的过期时间
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

// 使用多级缓存
Local Cache → Redis → Database
```

### JVM 优化（JDK 21）

```bash
# 启用虚拟线程
-Djdk.virtualThreadScheduler.parallelism=8

# G1 GC
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

---

## 🚀 部署清单

### 前置条件

- [ ] JDK 21 已安装
- [ ] MySQL 8.3 已启动
- [ ] Redis 7.x 已启动
- [ ] Nacos 2.x 已启动
- [ ] RocketMQ 已启动（可选）
- [ ] Seata Server 已启动（可选）

### 启动顺序

1. Nacos（注册中心 + 配置中心）
2. MySQL + Redis
3. eos-gateway（网关）
4. eos-service-user（用户服务）
5. eos-service-product（商品服务）
6. eos-service-order（订单服务）

### 健康检查

```bash
# 网关
curl http://localhost:8080/actuator/health

# 用户服务
curl http://localhost:8081/actuator/health

# 订单服务
curl http://localhost:8082/actuator/health
```

---

## 📖 相关文档

- [完整学习指南](LEARNING_GUIDE.md) - 详细的技术讲解
- [项目设计文档](PROJECT_DESIGN.md) - 架构设计与业务场景
- [README](README.md) - 项目介绍与快速开始

---

**最后更新**：2024-04-15  
**维护者**：EOS Team
