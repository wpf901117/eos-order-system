# EOS 企业级订单系统 - 技术学习指南

> **项目定位**：面向 10 年 Java 开发经验的技术提升训练项目  
> **技术栈**：JDK 21 + Spring Boot 3.2 + Spring Cloud 2023 + DDD + 微服务架构  
> **学习目标**：掌握企业级微服务系统的核心技术与最佳实践

---

## 📚 目录

- [一、项目概述](#一项目概述)
- [二、技术架构](#二技术架构)
- [三、核心模块详解](#三核心模块详解)
- [四、高并发防护体系](#四高并发防护体系)
- [五、分布式事务](#五分布式事务)
- [六、DDD 领域驱动设计](#六ddd-领域驱动设计)
- [七、JDK 21 新特性](#七jdk-21-新特性)
- [八、弹性设计与容错](#八弹性设计与容错)
- [九、可观测性与监控](#九可观测性与监控)
- [十、最佳实践](#十最佳实践)
- [十一、学习路径](#十一学习路径)

---

## 一、项目概述

### 1.1 项目背景

EOS（Enterprise Order System）是一个模拟真实企业场景的订单管理系统，采用微服务架构，涵盖从用户管理、商品管理到订单处理的全业务流程。

### 1.2 核心价值

本项目旨在通过一个完整的微服务项目，帮助开发者掌握：
- ✅ 现代 Java 技术栈（JDK 21 + Spring Boot 3.x）
- ✅ 微服务架构设计与实现
- ✅ 高并发场景下的系统防护
- ✅ 分布式事务解决方案
- ✅ DDD 领域驱动设计实战
- ✅ 企业级代码规范与最佳实践

### 1.3 项目结构

```
enterprise-order-system/
├── eos-common/                    # 公共模块
│   ├── eos-common-core/          # 核心工具类、注解、切面
│   ├── eos-common-mybatis/       # MyBatis Plus 配置
│   └── eos-common-redis/         # Redis 配置
├── eos-gateway/                  # API 网关
│   └── filter/AuthFilter.java    # 统一认证过滤器
├── eos-service/                  # 业务服务
│   ├── eos-service-user/         # 用户服务
│   ├── eos-service-product/      # 商品服务
│   └── eos-service-order/        # 订单服务（核心）
└── scripts/                      # 数据库脚本
```

---

## 二、技术架构

### 2.1 技术栈总览

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **基础框架** | JDK | 21 LTS | 支持虚拟线程、Record、Pattern Matching |
| | Spring Boot | 3.2.5 | 微服务基石 |
| | Spring Cloud | 2023.0.1 | 微服务生态 |
| | Spring Cloud Alibaba | 2023.0.1.0 | 国内微服务解决方案 |
| **数据存储** | MySQL | 8.3 | 关系型数据库 |
| | MyBatis Plus | 3.5.6 | ORM 框架增强 |
| | Redis | 7.x | 缓存、分布式锁 |
| | Redisson | 3.27.2 | Redis 客户端 |
| **消息队列** | RocketMQ | 2.3.0 | 事务消息、延迟消息 |
| **服务治理** | Nacos | 2.x | 注册中心、配置中心 |
| | Sentinel | 2023.x | 流量控制、熔断降级 |
| | Seata | 2023.x | 分布式事务 |
| **API 网关** | Spring Cloud Gateway | 2023.x | 路由、鉴权、限流 |
| **工具库** | Lombok | 1.18.32 | 简化代码 |
| | MapStruct | 1.5.5 | 对象映射 |
| | Hutool | 5.8.26 | Java 工具库 |
| | JWT (jjwt) | 0.12.5 | Token 生成与验证 |

### 2.2 架构图

```
┌─────────────────────────────────────────────────────┐
│                   客户端（Web/Mobile）                 │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              API 网关（Spring Cloud Gateway）          │
│   ├─ 统一认证（JWT + Session）                        │
│   ├─ 请求路由                                         │
│   ├─ 限流熔断                                         │
│   └─ 日志记录                                         │
└─────────────────────────────────────────────────────┘
            ↓                    ↓                    ↓
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   用户服务        │  │   商品服务        │  │   订单服务        │
│  (eos-user)      │  │  (eos-product)   │  │  (eos-order)     │
│                  │  │                  │  │                  │
│ • 用户注册       │  │ • 商品管理       │  │ • 订单创建       │
│ • 用户登录       │  │ • 库存管理       │  │ • 订单支付       │
│ • Token 刷新     │  │ • 商品查询       │  │ • 订单取消       │
│ • 分布式锁       │  │                  │  │ • 分布式事务     │
└──────────────────┘  └──────────────────┘  └──────────────────┘
            ↓                    ↓                    ↓
┌─────────────────────────────────────────────────────┐
│                   基础设施层                           │
│   ├─ MySQL（主从复制、分库分表）                       │
│   ├─ Redis（缓存、分布式锁、Session）                  │
│   ├─ RocketMQ（事务消息、延迟消息）                    │
│   ├─ Nacos（服务发现、配置中心）                       │
│   └─ Seata（分布式事务协调）                          │
└─────────────────────────────────────────────────────┘
```

---

## 三、核心模块详解

### 3.1 统一认证模块（Gateway）

**位置**：`eos-gateway/src/main/java/com/eos/gateway/filter/AuthFilter.java`

**核心功能**：
- JWT Token 验证
- Session 会话管理
- 用户信息透传
- 白名单放行

**关键技术点**：

#### 3.1.1 双 Token 机制（Access + Refresh）

```java
// Access Token：短期有效（30分钟）
String accessToken = JwtUtil.generateAccessToken(claims, sessionId);

// Refresh Token：长期有效（7天）
String refreshToken = JwtUtil.generateRefreshToken(claims, sessionId);
```

**优势**：
- 安全性：Access Token 泄露影响时间短
- 用户体验：Refresh Token 实现无感续期
- 可撤销：通过删除 Session 立即失效

#### 3.1.2 Session + JWT 混合方案

```
JWT (Access Token)          Redis Session
├─ userId                   ├─ userId
├─ username                 ├─ username
├─ role                     ├─ role
└─ sessionId (sid)          ├─ refreshToken
                            ├─ lastAccessTime
                            └─ TTL: 7天（滑动过期）
```

**工作流程**：
1. 登录时生成 sessionId，存入 JWT 和 Redis
2. 网关验证 JWT 签名后，检查 Redis 中 Session 是否存在
3. Session 不存在 → Token 已撤销（登出）
4. Session 存在 → 解析用户信息，透传给下游服务

**代码示例**：

```java
// AuthFilter.java - 网关认证逻辑
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 1. 白名单放行
    if (isWhiteList(path)) {
        return chain.filter(exchange);
    }

    // 2. 获取并验证 Token
    String token = JwtUtil.extractToken(authHeader);
    if (!JwtUtil.validateToken(token)) {
        return unauthorized(response, "Token无效或已过期");
    }

    // 3. 检查 Session 是否被撤销
    String sessionId = JwtUtil.getSessionId(token);
    String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
    
    return reactiveRedisTemplate.hasKey(sessionKey)
        .flatMap(sessionExists -> {
            if (!Boolean.TRUE.equals(sessionExists)) {
                return unauthorized(response, "会话已失效");
            }
            
            // 4. 解析用户信息并透传
            Claims claims = JwtUtil.parseToken(token);
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(CommonConstant.HEADER_USER_ID, claims.get("userId"))
                .header(CommonConstant.HEADER_USERNAME, claims.get("username"))
                .header(CommonConstant.HEADER_ROLE, claims.get("role"))
                .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        });
}
```

**学习要点**：
- ✅ JWT 的结构与原理（Header.Payload.Signature）
- ✅ Token 签名验证机制
- ✅ Session 撤销实现登出功能
- ✅ 滑动过期提升用户体验
- ✅ 网关统一鉴权，下游服务无需重复验证

---

### 3.2 用户服务（User Service）

**位置**：`eos-service/eos-service-user/`

**核心功能**：
- 用户注册（BCrypt 密码加密）
- 用户登录（生成双 Token）
- Token 刷新（滑动过期）
- 用户登出（撤销 Session）

**关键技术点**：

#### 3.2.1 BCrypt 密码加密

```java
// SecurityConfig.java
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// UserServiceImpl.java - 注册时加密
user.setPassword(passwordEncoder.encode(dto.getPassword()));

// 登录时验证
if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
    throw new BizException(ResultCode.LOGIN_ERROR);
}
```

**优势**：
- 自带随机 Salt，防彩虹表攻击
- 可调节工作因子（默认 2^10 次迭代）
- 单向哈希，不可逆

#### 3.2.2 Token 刷新机制

```java
// UserServiceImpl.java
@Override
public TokenVO refresh(String refreshToken) {
    // 1. 验证 RefreshToken 有效性
    if (!JwtUtil.validateToken(refreshToken)) {
        throw new BizException(ResultCode.TOKEN_INVALID);
    }

    // 2. 校验 Token 类型
    String tokenType = JwtUtil.getTokenType(refreshToken);
    if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenType)) {
        throw new BizException(ResultCode.TOKEN_INVALID, "非法的刷新令牌");
    }

    // 3. 检查 Session 是否存在
    String sessionId = JwtUtil.getSessionId(refreshToken);
    String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
    String userId = hashOps.get(sessionKey, "userId");
    
    if (userId == null) {
        throw new BizException(ResultCode.TOKEN_INVALID, "会话已失效");
    }

    // 4. 滑动过期：更新访问时间，重置 TTL
    hashOps.put(sessionKey, "lastAccessTime", String.valueOf(System.currentTimeMillis()));
    redisTemplate.expire(sessionKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);

    // 5. 生成新的 Access Token（保持同一 sessionId）
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", Long.valueOf(userId));
    claims.put("username", hashOps.get(sessionKey, "username"));
    claims.put("role", hashOps.get(sessionKey, "role"));
    
    String accessToken = JwtUtil.generateAccessToken(claims, sessionId);

    // 6. 返回新 Token（Refresh Token 不变）
    return new TokenVO(accessToken, refreshToken, ACCESS_TOKEN_EXPIRE_SECONDS);
}
```

**学习要点**：
- ✅ BCrypt 加密原理与使用
- ✅ 双 Token 机制的设计与实现
- ✅ 滑动过期策略
- ✅ Session 保活机制

---

## 四、高并发防护体系

### 4.1 幂等性防护

**位置**：
- 注解：`eos-common/eos-common-core/src/main/java/com/eos/common/annotation/Idempotent.java`
- 切面：`eos-common/eos-common-core/src/main/java/com/eos/common/aspect/IdempotentAspect.java`

**实现原理**：基于 Redis SET NX 原子操作

**使用方式**：

```java
@PostMapping("/create")
@Idempotent(key = "#userId + ':' + #dto.productId", expire = 30)
public Result<OrderVO> createOrder(@RequestHeader Long userId,
                                   @RequestBody OrderCreateDTO dto) {
    return orderService.createOrder(userId, dto);
}
```

**核心代码**：

```java
@Around("@annotation(idempotent)")
public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
    // 1. 解析幂等键（支持 SpEL 表达式）
    String key = parseKey(idempotent.key(), joinPoint);
    String idempotentKey = IDEMPOTENT_PREFIX + key;

    // 2. SET NX 原子操作
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(idempotentKey, "1", idempotent.expire(), TimeUnit.SECONDS);

    if (Boolean.FALSE.equals(success)) {
        throw new BizException(ResultCode.DUPLICATE_REQUEST, idempotent.message());
    }

    try {
        // 3. 执行业务逻辑
        return joinPoint.proceed();
    } catch (Throwable e) {
        // 4. 业务异常时删除幂等键，允许重试
        redisTemplate.delete(idempotentKey);
        throw e;
    }
}
```

**应用场景**：
- 订单创建（防止重复提交）
- 支付请求（防止重复支付）
- 表单提交（防止网络抖动导致重复）

**学习要点**：
- ✅ Redis SET NX 原子性保证
- ✅ SpEL 表达式解析
- ✅ AOP 切面编程
- ✅ 异常时的幂等键清理

---

### 4.2 限流防护

**位置**：
- 注解：`eos-common/eos-common-core/src/main/java/com/eos/common/annotation/RateLimit.java`
- 切面：`eos-common/eos-common-core/src/main/java/com/eos/common/aspect/RateLimitAspect.java`

**实现原理**：Redis + Lua 脚本实现原子性滑动窗口计数

**使用方式**：

```java
@PostMapping("/create")
@RateLimit(key = "#userId", maxRequests = 10, windowSeconds = 60)
public Result<OrderVO> createOrder(@RequestHeader Long userId,
                                   @RequestBody OrderCreateDTO dto) {
    return orderService.createOrder(userId, dto);
}
```

**Lua 脚本**：

```lua
local key = KEYS[1]
local window = tonumber(ARGV[1])
local maxRequests = tonumber(ARGV[2])
local current = redis.call('GET', key)

if current and tonumber(current) >= maxRequests then
    return 0  -- 超过限制
end

current = redis.call('INCR', key)
if tonumber(current) == 1 then
    redis.call('EXPIRE', key, window)  -- 设置过期时间
end

return 1  -- 允许通过
```

**学习要点**：
- ✅ Lua 脚本保证原子性
- ✅ 滑动窗口算法
- ✅ 高性能限流（无竞态条件）

---

### 4.3 分布式锁

**位置**：`eos-common/eos-common-core/src/main/java/com/eos/common/util/DistributedLockUtil.java`

**实现原理**：基于 Redisson 实现

**使用方式**：

```java
// 方式1：手动加锁
String lockKey = "order:create:" + userId + ":" + productId;
RLock lock = DistributedLockUtil.lock(redissonClient, lockKey);
try {
    if (lock == null) {
        throw new BizException(ResultCode.DISTRIBUTED_LOCK_ERROR);
    }
    // 业务逻辑
    doSomething();
} finally {
    DistributedLockUtil.unlock(lock);
}

// 方式2：函数式编程（推荐）
DistributedLockUtil.executeWithLock(redissonClient, lockKey, () -> {
    // 业务逻辑
    return result;
});
```

**核心代码**：

```java
public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey,
                                    long waitTime, long leaseTime, TimeUnit unit,
                                    Supplier<T> supplier) {
    RLock lock = null;
    try {
        lock = lock(redissonClient, lockKey, waitTime, leaseTime, unit);
        if (lock == null) {
            throw new RuntimeException("获取分布式锁失败: " + lockKey);
        }
        return supplier.get();
    } finally {
        unlock(lock);  // 自动释放锁
    }
}
```

**应用场景**：
- 库存扣减（防止超卖）
- 订单创建（防止重复提交）
- 定时任务（防止重复执行）

**学习要点**：
- ✅ Redisson 分布式锁原理
- ✅ 看门狗机制（自动续期）
- ✅ 函数式编程简化锁的使用
- ✅ 防止死锁的最佳实践

---

## 五、分布式事务

### 5.1 Seata AT 模式

**位置**：`eos-service/eos-service-order/src/main/resources/application-seata.yml`

**配置示例**：

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: eos_tx_group
  enable-auto-data-source-proxy: true
  config:
    type: nacos
    nacos:
      server-addr: ${NACOS_ADDR:127.0.0.1:8848}
      namespace: seata
```

**使用方式**：

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
@Transactional(rollbackFor = Exception.class)
public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
    // 1. 扣减库存（远程调用）
    inventoryFeignClient.deductStock(dto.getProductId(), dto.getQuantity());
    
    // 2. 创建订单（本地事务）
    Order order = buildOrder(dto);
    orderMapper.insert(order);
    
    // 3. 扣减余额（远程调用）
    accountFeignClient.deductBalance(dto.getUserId(), order.getAmount());
    
    // 任一环节失败，全局回滚
    return convertToVO(order);
}
```

**工作原理**：
1. TC（Transaction Coordinator）：事务协调器
2. TM（Transaction Manager）：事务管理器（发起方）
3. RM（Resource Manager）：资源管理器（参与方）
4. 两阶段提交：Prepare → Commit/Rollback

**学习要点**：
- ✅ Seata AT 模式原理
- ✅ 全局事务与本地事务的关系
- ✅ 回滚日志（Undo Log）机制
- ✅ 适用场景：强一致性要求

---

### 5.2 RocketMQ 事务消息

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/mq/OrderCreatedTransactionListener.java`

**实现原理**：最终一致性方案

**工作流程**：
1. 发送半消息（Half Message）到 MQ
2. 执行本地事务（创建订单）
3. 根据本地事务结果提交或回滚消息
4. MQ 主动回查未确认的事务

**核心代码**：

```java
@RocketMQTransactionListener
@Component
public class OrderCreatedTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 执行本地事务
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Long orderId = (Long) arg;
        
        // 查询订单是否存在且状态正确
        Order order = orderMapper.selectById(orderId);
        
        if (order != null && order.getStatus() == 0) {
            return RocketMQLocalTransactionState.COMMIT;
        } else {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务回查（网络异常时 MQ 主动询问）
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Long orderId = msg.getHeaders().get("orderId", Long.class);
        Order order = orderMapper.selectById(orderId);
        
        if (order != null && order.getStatus() == 0) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

**学习要点**：
- ✅ 事务消息的工作原理
- ✅ 半消息机制
- ✅ 事务回查机制
- ✅ 适用场景：最终一致性要求

---

## 六、DDD 领域驱动设计

### 6.1 充血模型 vs 贫血模型

**传统贫血模型**：
```java
// Entity 只有数据
@Data
public class Order {
    private Long id;
    private BigDecimal amount;
    private Integer status;
}

// Service 包含所有逻辑
@Service
public class OrderServiceImpl {
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order.getStatus() != 0) {
            throw new BizException("订单状态不正确");
        }
        order.setStatus(3);
        orderMapper.updateById(order);
    }
}
```

**DDD 充血模型**：
```java
// 聚合根：包含业务逻辑
@Getter
public class Order {
    private OrderStatus status;
    private Money totalAmount;
    
    /**
     * 领域行为：取消订单
     */
    public void cancel() {
        // 业务规则封装在领域对象内部
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new BizException("订单状态不允许取消");
        }
        
        this.status = OrderStatus.CANCELLED;
        this.cancelTime = LocalDateTime.now();
        
        // 发布领域事件
        DomainEventPublisher.publish(new OrderCancelledEvent(this));
    }
}

// 应用服务：只负责编排
@Service
public class OrderApplicationService {
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("订单不存在"));
        
        order.cancel();  // 调用领域行为
        
        orderRepository.save(order);
    }
}
```

### 6.2 核心概念

#### 6.2.1 聚合根（Aggregate Root）

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/domain/Order.java`

**特点**：
- 聚合的入口点
- 保证聚合内的一致性
- 外部只能通过聚合根访问聚合内的对象

**示例**：

```java
public class Order {
    private List<OrderItem> items;  // 子实体
    
    /**
     * 工厂方法：创建订单
     */
    public static Order create(Long userId, Long productId, String productName,
                                Integer quantity, Money unitPrice, String address) {
        // 业务规则校验
        if (quantity <= 0) {
            throw new BizException("购买数量必须大于0");
        }
        
        Order order = new Order();
        order.userId = userId;
        order.totalAmount = unitPrice.multiply(quantity);
        order.status = OrderStatus.PENDING_PAYMENT;
        
        return order;
    }
    
    /**
     * 领域行为：支付
     */
    public void pay() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new BizException("订单状态不允许支付");
        }
        
        this.status = OrderStatus.PAID;
        this.payTime = LocalDateTime.now();
    }
}
```

#### 6.2.2 值对象（Value Object）

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/domain/Money.java`

**特点**：
- 不可变性（Immutable）
- 通过值相等性判断（而非引用）
- 无身份标识

**示例**：

```java
@Getter
@EqualsAndHashCode
public class Money {
    private final BigDecimal amount;
    private final String currency;
    
    public Money(BigDecimal amount, String currency) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency;
    }
    
    /**
     * 加法运算（返回新对象）
     */
    public Money add(Money other) {
        checkCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * 乘法运算
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(new BigDecimal(multiplier)), this.currency);
    }
}
```

**使用示例**：

```java
Money price1 = new Money(new BigDecimal("100"), "CNY");
Money price2 = new Money(new BigDecimal("50"), "CNY");
Money total = price1.add(price2);  // 150 CNY
```

#### 6.2.3 领域事件（Domain Event）

**概念**：领域中发生的重要事情

**示例**：

```java
// 订单创建事件
public class OrderCreatedEvent {
    private Long orderId;
    private String orderNo;
    private LocalDateTime createTime;
}

// 发布事件
DomainEventPublisher.publish(new OrderCreatedEvent(order));

// 监听事件
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    // 发送通知
    // 更新统计
    // 触发其他业务流程
}
```

### 6.3 DDD 分层架构

```
┌─────────────────────────────────────┐
│       接口层（Interface）             │
│   Controller / REST API             │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│      应用层（Application）            │
│   Application Service               │
│   • 事务管理                         │
│   • 权限校验                         │
│   • 协调领域对象                     │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│       领域层（Domain）               │
│   • Entity / Aggregate Root         │
│   • Value Object                    │
│   • Domain Service                  │
│   • Domain Event                    │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│     基础设施层（Infrastructure）      │
│   • Repository Implementation       │
│   • External Service Adapter        │
│   • Message Queue Producer          │
└─────────────────────────────────────┘
```

**学习要点**：
- ✅ 充血模型的优势
- ✅ 聚合根的设计原则
- ✅ 值对象的不可变性
- ✅ 领域事件的解耦作用
- ✅ DDD 分层架构的职责划分

---

## 七、JDK 21 新特性

### 7.1 虚拟线程（Virtual Threads）

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/config/VirtualThreadConfig.java`

**特性**：
- 轻量级线程（几KB vs 1MB）
- 可创建数百万个线程
- 适合 I/O 密集型任务

**使用方式**：

```java
@Configuration
public class VirtualThreadConfig {
    
    @Bean("virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

// 使用示例
@Service
public class OrderBatchService {
    
    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;
    
    public void sendNotifications(List<Long> orderIds) {
        orderIds.forEach(orderId -> {
            virtualThreadExecutor.submit(() -> {
                // 每个任务在一个虚拟线程中执行
                sendNotification(orderId);
            });
        });
    }
}
```

**性能对比**：

| 指标 | 平台线程 | 虚拟线程 |
|------|---------|---------|
| 内存占用 | ~1MB/线程 | ~几KB/线程 |
| 最大线程数 | ~几千 | ~百万 |
| 上下文切换 | 重量级 | 轻量级 |
| 适用场景 | CPU 密集型 | I/O 密集型 |

**学习要点**：
- ✅ 虚拟线程的原理
- ✅ 何时使用虚拟线程
- ✅ 与传统线程池的对比

---

### 7.2 Record 类

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/dto/OrderStatisticsDTO.java`

**特性**：
- 不可变数据载体
- 自动生成 constructor、equals、hashCode、toString
- 代码简洁

**使用方式**：

```java
public record OrderStatisticsDTO(
    Long userId,
    Integer totalOrders,
    BigDecimal totalAmount,
    Integer paidOrders,
    Integer unpaidOrders
) {
    // 紧凑构造函数：添加验证逻辑
    public OrderStatisticsDTO {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID必须为正数");
        }
    }
    
    // 工厂方法
    public static OrderStatisticsDTO empty(Long userId) {
        return new OrderStatisticsDTO(userId, 0, BigDecimal.ZERO, 0, 0);
    }
}

// 使用示例
OrderStatisticsDTO stats = new OrderStatisticsDTO(userId, 10, 
    new BigDecimal("1000"), 8, 2);
```

**对比传统 Class**：

```java
// 传统方式（需要 50+ 行代码）
@Data
@AllArgsConstructor
public class OrderStatisticsDTO {
    private Long userId;
    private Integer totalOrders;
    private BigDecimal totalAmount;
    private Integer paidOrders;
    private Integer unpaidOrders;
}

// Record 方式（只需 10 行代码）
public record OrderStatisticsDTO(
    Long userId, Integer totalOrders, 
    BigDecimal totalAmount, 
    Integer paidOrders, Integer unpaidOrders
) {}
```

**学习要点**：
- ✅ Record 的不可变性
- ✅ 紧凑构造函数的使用
- ✅ 适用场景：DTO、值对象

---

### 7.3 Pattern Matching for Switch

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/service/OrderStatisticsService.java`

**特性**：
- 更简洁的条件判断
- 编译时检查 exhaustiveness
- 支持 null 安全处理
- Guarded patterns（条件模式匹配）

**使用方式**：

```java
// 传统 switch
public String getStatusDescription(Integer status) {
    if (status == null) {
        return "未知";
    }
    switch (status) {
        case 0: return "待支付";
        case 1: return "已支付";
        default: return "其他";
    }
}

// JDK 21 Switch 表达式
public String getStatusDescription(Integer status) {
    return switch (status) {
        case 0 -> "待支付";
        case 1 -> "已支付";
        case null, default -> "未知状态";
    };
}

// Guarded Patterns（条件模式匹配）
public BigDecimal calculateDiscount(BigDecimal amount, String userType) {
    return switch (userType) {
        case "VIP" when amount.compareTo(new BigDecimal("1000")) > 0 -> 
            amount.multiply(new BigDecimal("0.8"));  // VIP且金额>1000，8折
        case "VIP" -> 
            amount.multiply(new BigDecimal("0.9"));  // VIP，9折
        case "NORMAL" when amount.compareTo(new BigDecimal("500")) > 0 -> 
            amount.multiply(new BigDecimal("0.95")); // 普通用户且金额>500，95折
        case null, default -> 
            throw new BizException("无效的用户类型");
    };
}

// Pattern Matching for instanceof
public String processOrder(Object obj) {
    return switch (obj) {
        case Order o when o.getStatus() == 0 -> 
            "待支付订单: " + o.getOrderNo();
        case Order o -> 
            "其他状态订单";
        case null -> 
            "订单为空";
        default -> 
            "非订单对象";
    };
}
```

**学习要点**：
- ✅ Switch 表达式的语法
- ✅ Guarded patterns 的使用
- ✅ null 安全处理
- ✅ 模式匹配的类型检查

---

## 八、弹性设计与容错

### 8.1 Sentinel 流量控制

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/config/SentinelConfig.java`

**配置示例**：

```java
@Configuration
public class SentinelConfig {
    
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // 订单创建接口：QPS 限制为 100
        FlowRule orderCreateRule = new FlowRule();
        orderCreateRule.setResource("createOrder");
        orderCreateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderCreateRule.setCount(100);
        rules.add(orderCreateRule);
        
        FlowRuleManager.loadRules(rules);
    }
}
```

### 8.2 熔断降级

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/service/ResilientOrderService.java`

**使用方式**：

```java
@Service
public class ResilientOrderService {
    
    @SentinelResource(
        value = "getProductById",
        blockHandler = "handleBlockException",
        fallback = "handleFallback"
    )
    public ProductVO getProductWithFallback(Long productId) {
        var result = productFeignClient.getProductById(productId);
        return result.getData();
    }
    
    // 流控处理
    public ProductVO handleBlockException(Long productId, BlockException ex) {
        log.warn("[流控] 商品查询接口被限流");
        return null;
    }
    
    // 降级处理（服务不可用）
    public ProductVO handleFallback(Long productId, Throwable t) {
        log.error("[降级] 商品服务不可用");
        // 返回缓存数据或默认值
        return getDefaultProduct(productId);
    }
}
```

**熔断策略**：
1. **慢调用比例**：响应时间超过阈值的比例
2. **异常比例**：异常数的比例
3. **异常数**：异常数的绝对值

**学习要点**：
- ✅ 流量控制的三种模式（QPS、线程数、关联）
- ✅ 熔断降级的触发条件
- ✅ BlockHandler vs Fallback 的区别
- ✅ 降级策略的设计

---

## 九、可观测性与监控

### 9.1 Actuator + Prometheus 监控

**位置**：`eos-service/eos-service-order/src/main/java/com/eos/order/monitor/OrderMetrics.java`

**核心功能**：
- 应用健康检查
- 指标采集与导出
- 自定义业务指标

**配置示例**：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**自定义业务指标**：

```java
@Component
public class OrderMetrics {
    
    private final MeterRegistry meterRegistry;
    private Counter orderCreateCounter;
    private Timer orderCreateTimer;
    
    @PostConstruct
    public void init() {
        // 计数器：订单创建总数
        orderCreateCounter = Counter.builder("order.create.total")
                .description("订单创建总数")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // 计时器：订单创建耗时
        orderCreateTimer = Timer.builder("order.create.duration")
                .description("订单创建耗时")
                .register(meterRegistry);
    }
    
    public void recordOrderCreate() {
        orderCreateCounter.increment();
    }
    
    public void recordOrderCreateDuration(long durationMs) {
        orderCreateTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

**使用方式**：

```java
@Service
public class OrderServiceImpl {
    
    @Autowired
    private OrderMetrics orderMetrics;
    
    public OrderVO createOrder(...) {
        long startTime = System.currentTimeMillis();
        
        // 业务逻辑
        OrderVO order = doCreateOrder(...);
        
        // 记录监控指标
        long duration = System.currentTimeMillis() - startTime;
        orderMetrics.recordOrderCreate();
        orderMetrics.recordOrderCreateDuration(duration);
        
        return order;
    }
}
```

**访问监控数据**：

```bash
# 健康检查
curl http://localhost:8083/actuator/health

# Prometheus 指标
curl http://localhost:8083/actuator/prometheus
```

**Prometheus 查询语句**：

```promql
# 订单创建速率（QPS）
rate(order_create_total[5m])

# 订单创建平均耗时
rate(order_create_duration_sum[5m]) / rate(order_create_duration_count[5m])

# 待支付订单数
order_pending_count
```

### 9.2 SkyWalking 分布式链路追踪

**为什么需要 SkyWalking？**
- ✅ 全链路追踪：跨服务的调用链可视化
- ✅ 性能分析：慢接口自动识别
- ✅ 拓扑图：服务依赖关系自动发现
- ✅ 告警：异常自动通知

**实施步骤**：

#### 步骤1：启动 SkyWalking

```bash
# 下载
wget https://archive.apache.org/dist/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz
tar -xzf apache-skywalking-apm-9.7.0.tar.gz

# 启动 OAP Server
cd apache-skywalking-apm-bin
bin/oapService.sh

# 启动 UI
bin/webappService.sh
```

访问：http://localhost:8080

#### 步骤2：配置 Java Agent

**IDEA 配置**：
```
VM Options:
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=eos-order-service
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

**启动脚本**：
```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=eos-order-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar eos-service-order.jar
```

#### 步骤3：查看追踪链路

1. 调用几个接口
2. 访问 SkyWalking UI：http://localhost:8080
3. 点击“拓扑图”查看服务依赖
4. 点击“追踪”查看调用链详情

### 9.3 ELK 日志聚合

**架构**：
```
应用日志 → Filebeat → Logstash → Elasticsearch → Kibana
```

**docker-compose-elk.yml**：

```yaml
version: '3.8'

services:
  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"

  kibana:
    image: kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200

  logstash:
    image: logstash:8.11.0
    ports:
      - "5044:5044"
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

  filebeat:
    image: elastic/filebeat:8.11.0
    volumes:
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml
      - /var/log/app:/var/log/app:ro
```

**学习要点**：
- ✅ 监控四黄金信号（延迟、流量、错误、饱和度）
- ✅ RED 方法（Rate、Errors、Duration）
- ✅ USE 方法（Utilization、Saturation、Errors）
- ✅ 分布式链路追踪原理
- ✅ 日志聚合与分析

---

## 十、最佳实践

### 9.1 代码规范

#### 9.1.1 命名规范

```java
// ✅ 好的命名
public class OrderApplicationService { }
public interface OrderRepository { }
public record OrderStatisticsDTO { }
public enum OrderStatus { }

// ❌ 不好的命名
public class OrderService { }  // 太泛
public interface OrderDao { }  // 应该用 Repository
public class OrderDto { }      // 应该明确用途
```

#### 9.1.2 异常处理

```java
// ✅ 好的做法：统一异常处理
@GetMapping("/{orderId}")
public Result<OrderVO> getOrder(@PathVariable Long orderId) {
    OrderVO order = orderService.getOrderById(orderId);
    return Result.ok(order);
}

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}

// ❌ 不好的做法：吞掉异常
try {
    doSomething();
} catch (Exception e) {
    // 什么都不做
}
```

### 9.2 性能优化

#### 9.2.1 数据库优化

```sql
-- ✅ 好的索引设计
CREATE INDEX idx_user_status_time 
ON t_order(user_id, status, create_time);

-- ✅ 覆盖索引避免回表
SELECT id, order_no, amount, status
FROM orders
WHERE user_id = ? AND status = ?;

-- ❌ 避免 SELECT *
SELECT * FROM orders WHERE user_id = ?;
```

#### 9.2.2 缓存策略

```java
// ✅ 多级缓存
public ProductVO getProduct(Long productId) {
    // L1: 本地缓存
    ProductVO product = localCache.getIfPresent(productId);
    if (product != null) {
        return product;
    }
    
    // L2: Redis 缓存
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

### 9.3 安全实践

#### 9.3.1 SQL 注入防护

```java
// ✅ 使用参数化查询
LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Order::getUserId, userId);
orderMapper.selectList(wrapper);

// ❌ 拼接 SQL（危险）
String sql = "SELECT * FROM orders WHERE user_id = " + userId;
```

#### 9.3.2 敏感信息保护

```java
// ✅ 密码加密存储
user.setPassword(passwordEncoder.encode(dto.getPassword()));

// ✅ 手机号脱敏
private String maskPhone(String phone) {
    return phone.substring(0, 3) + "****" + phone.substring(7);
}

// ❌ 明文存储密码
user.setPassword(dto.getPassword());
```

---

## 十一、学习路径

### 11.1 初级开发者（1-3年）

**学习目标**：掌握基础微服务架构

**学习顺序**：
1. ✅ Spring Boot 基础
2. ✅ MyBatis Plus 使用
3. ✅ RESTful API 设计
4. ✅ JWT 认证原理
5. ✅ Redis 基本使用

**实践任务**：
- 完成用户注册/登录功能
- 实现简单的 CRUD 接口
- 理解网关的基本作用

---

### 11.2 中级开发者（3-5年）

**学习目标**：掌握高并发与分布式

**学习顺序**：
1. ✅ 幂等性设计
2. ✅ 分布式锁
3. ✅ 限流算法
4. ✅ 消息队列
5. ✅ 缓存策略

**实践任务**：
- 实现订单创建的幂等性
- 使用分布式锁防止超卖
- 集成 RocketMQ 发送消息

---

### 11.3 高级开发者（5-8年）

**学习目标**：掌握分布式事务与 DDD

**学习顺序**：
1. ✅ 分布式事务（Seata）
2. ✅ DDD 领域建模
3. ✅ 充血模型设计
4. ✅ 事件驱动架构
5. ✅ CQRS 模式

**实践任务**：
- 实现跨服务的分布式事务
- 重构订单模块为 DDD 架构
- 设计领域事件

---

### 11.4 资深开发者（8-10年+）

**学习目标**：掌握架构设计与性能优化

**学习顺序**：
1. ✅ 微服务拆分策略
2. ✅ 全链路监控
3. ✅ 性能调优（JVM、SQL）
4. ✅ 弹性设计（熔断、降级）
5. ✅ 云原生架构

**实践任务**：
- 设计完整的监控体系
- 进行全链路压测与调优
- 实现灰度发布策略

---

## 附录

### A. 常见问题

#### Q1: 为什么选择 Session + JWT 混合方案？

**A**: 
- 纯 JWT 无法主动撤销（除非黑名单）
- 纯 Session 不适合分布式环境
- 混合方案兼顾两者优势：无状态验证 + 可撤销

#### Q2: 什么时候使用 Seata，什么时候使用 RocketMQ 事务消息？

**A**:
- **Seata AT**：强一致性要求，跨多个服务的同步操作
- **RocketMQ 事务消息**：最终一致性要求，异步解耦场景

#### Q3: 虚拟线程适合所有场景吗？

**A**: 
- ✅ 适合：I/O 密集型（HTTP 请求、数据库查询）
- ❌ 不适合：CPU 密集型（复杂计算）

---

### B. 参考资源

**官方文档**：
- [Spring Boot 3.2](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [JDK 21](https://openjdk.org/projects/jdk/21/)
- [MyBatis Plus](https://baomidou.com/)
- [Redisson](https://redisson.org/)
- [Sentinel](https://sentinelguard.io/)
- [Seata](https://seata.io/)

**书籍推荐**：
- 《深入理解Java虚拟机》- 周志明
- 《领域驱动设计》- Eric Evans
- 《微服务架构设计模式》- Chris Richardson
- 《高性能MySQL》- Baron Schwartz

---

## 结语

本学习指南涵盖了 EOS 项目的核心技术点，建议按照学习路径循序渐进地掌握每个模块。记住：**理论与实践相结合**才是最好的学习方式。

祝学习愉快！🎉
