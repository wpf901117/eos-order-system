# EOS 项目 - 技术知识图谱

> 可视化展示各技术模块的依赖关系和学习路径

---

## 🗺️ 技术全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                        JDK 21 基础                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │虚拟线程  │  │ Record   │  │Pattern   │  │Switch        │   │
│  │          │  │          │  │Matching  │  │Expression    │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot 3.2 + Spring Cloud                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │Gateway   │  │OpenFeign │  │Nacos     │  │LoadBalancer  │   │
│  │网关      │  │服务调用  │  │注册/配置 │  │负载均衡      │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      数据存储层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │MySQL     │  │MyBatis   │  │Redis     │  │Redisson      │   │
│  │数据库    │  │Plus ORM  │  │缓存      │  │分布式锁      │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     消息中间件                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              RocketMQ                                     │   │
│  │  • 事务消息（最终一致性）                                  │   │
│  │  • 延迟消息（订单超时取消）                                │   │
│  │  • 顺序消息（保证处理顺序）                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    分布式事务                                     │
│  ┌──────────┐                    ┌──────────────────┐           │
│  │Seata AT  │                    │RocketMQ 事务消息 │           │
│  │强一致性  │                    │最终一致性        │           │
│  └──────────┘                    └──────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   高并发防护体系                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │幂等性    │  │限流      │  │分布式锁  │  │Sentinel      │   │
│  │@Idempotent│  │@RateLimit│  │Redisson  │  │熔断降级      │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  DDD 领域驱动设计                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │聚合根    │  │值对象    │  │领域事件  │  │应用服务      │   │
│  │Order     │  │Money     │  │Event     │  │Application   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔗 模块依赖关系图

### 1. 认证授权流程

```
客户端请求
    ↓
┌─────────────────────────────────┐
│  API Gateway (8080)             │
│  ┌───────────────────────────┐  │
│  │ AuthFilter                │  │
│  │ • 验证 JWT Token          │  │
│  │ • 检查 Redis Session      │  │
│  │ • 解析用户信息            │  │
│  │ • 透传 X-User-Id 等 Header│  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│  User Service (8081)            │
│  ┌───────────────────────────┐  │
│  │ UserController            │  │
│  │ • /login                  │  │
│  │ • /refresh                │  │
│  │ • /logout                 │  │
│  └───────────────────────────┘  │
│         ↓                       │
│  ┌───────────────────────────┐  │
│  │ UserService               │  │
│  │ • BCrypt 密码加密         │  │
│  │ • 生成双 Token            │  │
│  │ • Session 管理            │  │
│  └───────────────────────────┘  │
│         ↓                       │
│  ┌───────────────────────────┐  │
│  │ Redis                     │  │
│  │ • 存储 Session            │  │
│  │ • TTL: 7天（滑动过期）    │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**关键文件**：
- `eos-gateway/src/main/java/com/eos/gateway/filter/AuthFilter.java`
- `eos-common/eos-common-core/src/main/java/com/eos/common/util/JwtUtil.java`
- `eos-service/eos-service-user/src/main/java/com/eos/user/service/impl/UserServiceImpl.java`

---

### 2. 订单创建流程（含高并发防护）

```
客户端请求 POST /order/create
    ↓
┌─────────────────────────────────┐
│  API Gateway                    │
│  • 验证 Token                   │
│  • 透传用户信息                 │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│  Order Controller               │
│  @Idempotent(key="#userId:...") │ ← 幂等性检查
│  @RateLimit(key="#userId",...)  │ ← 限流检查
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│  Order Service                  │
│  DistributedLockUtil.execute    │ ← 分布式锁
│  WithLock(lockKey, () -> {      │
│    • 查询商品（Feign 调用）     │
│    • 金额校验                   │
│    • 扣减库存（远程调用）       │
│    • 创建订单（本地事务）       │
│    • 发送 MQ 消息（异步）       │
│  })                             │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│  数据库操作                      │
│  • INSERT INTO t_order          │
│  • UPDATE t_product SET stock   │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│  RocketMQ                       │
│  • 发送订单创建消息             │
│  • 下游服务异步处理             │
│    - 发送通知                   │
│    - 更新统计                   │
└─────────────────────────────────┘
```

**关键文件**：
- `eos-service/eos-service-order/src/main/java/com/eos/order/controller/OrderController.java`
- `eos-service/eos-service-order/src/main/java/com/eos/order/service/impl/OrderServiceImpl.java`
- `eos-common/eos-common-core/src/main/java/com/eos/common/aspect/IdempotentAspect.java`
- `eos-common/eos-common-core/src/main/java/com/eos/common/aspect/RateLimitAspect.java`
- `eos-common/eos-common-core/src/main/java/com/eos/common/util/DistributedLockUtil.java`

---

### 3. 分布式事务流程（Seata AT）

```
┌─────────────────────────────────────────────────────┐
│  TM: Order Service (发起方)                          │
│  @GlobalTransactional(name="create-order")          │
│                                                     │
│  1. 开始全局事务                                     │
│     ↓                                               │
│  2. 调用 Inventory Service (RM1)                    │
│     ├─ 本地事务：UPDATE stock                       │
│     ├─ 记录 Undo Log                                │
│     └─ 注册分支事务到 TC                             │
│     ↓                                               │
│  3. 调用 Account Service (RM2)                      │
│     ├─ 本地事务：UPDATE balance                     │
│     ├─ 记录 Undo Log                                │
│     └─ 注册分支事务到 TC                             │
│     ↓                                               │
│  4. 创建订单（本地事务）                             │
│     ├─ INSERT order                                 │
│     ├─ 记录 Undo Log                                │
│     └─ 注册分支事务到 TC                             │
│     ↓                                               │
│  5. 提交或回滚                                      │
│     ├─ 全部成功 → TC 通知所有 RM 提交               │
│     └─ 任一失败 → TC 通知所有 RM 回滚（使用 Undo Log）│
└─────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │Inventory │      │ Account  │      │  Order   │
    │ Service  │      │ Service  │      │ Service  │
    │ (RM1)    │      │ (RM2)    │      │ (RM3)    │
    └──────────┘      └──────────┘      └──────────┘
         ↓                    ↓                    ↓
    ┌──────────────────────────────────────────────────┐
    │         TC: Seata Server (事务协调器)             │
    │         • 管理全局事务状态                        │
    │         • 协调分支事务提交/回滚                   │
    └──────────────────────────────────────────────────┘
```

**关键文件**：
- `eos-service/eos-service-order/src/main/resources/application-seata.yml`
- `eos-service/eos-service-order/src/main/java/com/eos/order/service/impl/OrderServiceImpl.java`

---

### 4. 分布式事务流程（RocketMQ 事务消息）

```
┌─────────────────────────────────────────────────────┐
│  Order Service (生产者)                              │
│                                                     │
│  1. 发送半消息（Half Message）到 MQ                  │
│     ↓                                               │
│  2. 执行本地事务                                     │
│     ├─ 创建订单                                     │
│     └─ 返回事务状态（COMMIT/ROLLBACK）              │
│     ↓                                               │
│  3. 根据本地事务结果                                 │
│     ├─ 成功 → 提交消息（对消费者可见）              │
│     └─ 失败 → 回滚消息（删除）                      │
│     ↓                                               │
│  4. 如果 MQ 未收到确认，主动回查                     │
│     ├─ checkLocalTransaction()                      │
│     └─ 查询订单状态，决定提交/回滚                   │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│  RocketMQ                                           │
│  • 存储半消息（不可见）                              │
│  • 等待生产者确认                                    │
│  • 定时回查未确认的事务                              │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│  Consumer Service (消费者)                           │
│                                                     │
│  1. 接收订单创建消息                                 │
│  2. 执行业务逻辑                                     │
│     ├─ 发送通知（邮件、短信）                       │
│     ├─ 更新用户统计                                 │
│     └─ 触发其他业务流程                             │
│  3. 返回消费结果（ACK/NACK）                        │
└─────────────────────────────────────────────────────┘
```

**关键文件**：
- `eos-service/eos-service-order/src/main/java/com/eos/order/mq/OrderCreatedTransactionListener.java`
- `eos-service/eos-service-order/src/main/java/com/eos/order/mq/OrderCreatedMessage.java`

---

### 5. DDD 领域模型交互

```
┌─────────────────────────────────────────────────────┐
│  接口层（Controller）                                │
│  @PostMapping("/create")                            │
│  public Result<OrderVO> createOrder(...) {          │
│      return orderAppService.createOrder(...);       │
│  }                                                  │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│  应用层（Application Service）                       │
│  @Transactional                                     │
│  public Order createOrder(...) {                    │
│      // 1. 从仓储加载数据                           │
│      Product product = productRepository.findById();│
│                                                     │
│      // 2. 调用领域对象的业务方法                   │
│      Money price = new Money(product.getPrice());   │
│      Order order = Order.create(...);               │
│                                                     │
│      // 3. 持久化                                   │
│      orderRepository.save(order);                   │
│                                                     │
│      // 4. 发布领域事件                             │
│      eventPublisher.publish(new OrderCreatedEvent());│
│  }                                                  │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│  领域层（Domain）                                    │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │ Order（聚合根）                              │  │
│  │                                              │  │
│  │ • 工厂方法：create()                         │  │
│  │ • 领域行为：pay(), cancel(), ship()          │  │
│  │ • 业务规则：状态转换校验                      │  │
│  │ • 不变性约束：保证聚合内一致性                │  │
│  └──────────────────────────────────────────────┘  │
│         ↓                                          │
│  ┌──────────────────────────────────────────────┐  │
│  │ Money（值对象）                              │  │
│  │                                              │  │
│  │ • 不可变性                                   │  │
│  │ • 运算方法：add(), subtract(), multiply()    │  │
│  │ • 比较方法：compareTo()                      │  │
│  └──────────────────────────────────────────────┘  │
│         ↓                                          │
│  ┌──────────────────────────────────────────────┐  │
│  │ OrderStatus（枚举）                          │  │
│  │                                              │  │
│  │ • PENDING_PAYMENT                            │  │
│  │ • PAID                                       │  │
│  │ • SHIPPED                                    │  │
│  │ • COMPLETED                                  │  │
│  │ • CANCELLED                                  │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│  基础设施层（Infrastructure）                        │
│                                                     │
│  • OrderRepositoryImpl（仓储实现）                  │
│  • ProductFeignClient（外部服务适配器）             │
│  • RocketMQProducer（消息队列生产者）               │
└─────────────────────────────────────────────────────┘
```

**关键文件**：
- `eos-service/eos-service-order/src/main/java/com/eos/order/domain/Order.java`
- `eos-service/eos-service-order/src/main/java/com/eos/order/domain/Money.java`
- `eos-service/eos-service-order/src/main/java/com/eos/order/domain/OrderStatus.java`
- `eos-service/eos-service-order/src/main/java/com/eos/order/application/OrderApplicationService.java`

---

## 📚 学习路径图

### 阶段1：基础入门（1-2周）

```
Week 1: Java & Spring Boot 基础
├─ Day 1-2: JDK 21 新特性
│   ├─ 虚拟线程
│   ├─ Record 类
│   └─ Pattern Matching
├─ Day 3-4: Spring Boot 3.2
│   ├─ 自动配置原理
│   ├─ Starter 机制
│   └─ Actuator 监控
└─ Day 5: RESTful API 设计
    ├─ HTTP 方法
    ├─ 状态码
    └─ 统一响应格式

Week 2: 数据访问与缓存
├─ Day 1-2: MyBatis Plus
│   ├─ CRUD 操作
│   ├─ 条件构造器
│   └─ 分页插件
├─ Day 3-4: Redis
│   ├─ 数据结构
│   ├─ 缓存策略
│   └─ 分布式锁
└─ Day 5: JWT 认证
    ├─ Token 结构
    ├─ 签名验证
    └─ 双 Token 机制
```

---

### 阶段2：进阶提升（2-3周）

```
Week 3: 微服务架构
├─ Day 1-2: Spring Cloud Gateway
│   ├─ 路由配置
│   ├─ 过滤器链
│   └─ 统一鉴权
├─ Day 3-4: Nacos
│   ├─ 服务注册发现
│   ├─ 配置中心
│   └─ 动态刷新
└─ Day 5: OpenFeign
    ├─ 声明式调用
    ├─ 负载均衡
    └─ 熔断降级

Week 4: 高并发防护
├─ Day 1-2: 幂等性设计
│   ├─ Redis SET NX
│   ├─ AOP 切面
│   └─ SpEL 表达式
├─ Day 3-4: 限流算法
│   ├─ 滑动窗口
│   ├─ Lua 脚本
│   └─ Sentinel
└─ Day 5: 分布式锁
    ├─ Redisson
    ├─ 看门狗机制
    └─ 函数式编程

Week 5: 消息队列
├─ Day 1-2: RocketMQ 基础
│   ├─ 消息模型
│   ├─ 生产者/消费者
│   └─ 消息类型
├─ Day 3-4: 事务消息
│   ├─ 半消息机制
│   ├─ 事务回查
│   └─ 最终一致性
└─ Day 5: 实战演练
    ├─ 订单创建流程
    ├─ 异步通知
    └─ 解耦设计
```

---

### 阶段3：高级专题（3-4周）

```
Week 6-7: 分布式事务
├─ Day 1-2: Seata AT 模式
│   ├─ 两阶段提交
│   ├─ Undo Log
│   └─ 全局事务
├─ Day 3-4: RocketMQ 事务消息
│   ├─ 对比 Seata
│   ├─ 适用场景
│   └─ 最佳实践
└─ Day 5-7: 综合实战
    ├─ 跨服务订单流程
    ├─ 异常处理
    └─ 补偿机制

Week 8-9: DDD 领域驱动设计
├─ Day 1-2: DDD 理论基础
│   ├─ 战略设计
│   ├─ 战术设计
│   └─ 分层架构
├─ Day 3-4: 充血模型实战
│   ├─ 聚合根设计
│   ├─ 值对象
│   └─ 领域事件
└─ Day 5-7: 重构实践
    ├─ 贫血模型 → 充血模型
    ├─ 应用服务编排
    └─ 仓储模式

Week 10: 弹性设计与监控
├─ Day 1-2: Sentinel 深入
│   ├─ 流量控制
│   ├─ 熔断降级
│   └─ 热点参数限流
├─ Day 3-4: 全链路监控
│   ├─ SkyWalking
│   ├─ Prometheus
│   └─ Grafana
└─ Day 5-7: 性能优化
    ├─ JVM 调优
    ├─ SQL 优化
    └─ 缓存策略
```

---

## 🎯 核心知识点速记

### 认证授权
- **JWT**: Header.Payload.Signature（Base64 编码，非加密）
- **双 Token**: Access Token（30分钟）+ Refresh Token（7天）
- **Session**: Redis 存储，支持撤销登出
- **滑动过期**: 每次访问重置 TTL

### 高并发防护
- **幂等性**: Redis SET NX，原子操作
- **限流**: Lua 脚本，滑动窗口计数
- **分布式锁**: Redisson，看门狗自动续期
- **三层防护**: Controller（注解）→ Service（锁）→ DB（乐观锁）

### 分布式事务
- **Seata AT**: 强一致性，两阶段提交，Undo Log
- **RocketMQ**: 最终一致性，半消息，事务回查
- **选择原则**: 同步用 Seata，异步用 MQ

### DDD
- **聚合根**: 保证聚合内一致性，外部唯一入口
- **值对象**: 不可变，通过值相等，无身份标识
- **领域事件**: 解耦，异步，最终一致性
- **充血模型**: 业务逻辑在领域对象内部

### JDK 21
- **虚拟线程**: I/O 密集型，轻量级，百万并发
- **Record**: 不可变 DTO，自动生成方法
- **Pattern Matching**: Switch 表达式，Guarded Patterns

---

## 📖 推荐阅读顺序

1. **LEARNING_GUIDE.md** - 详细的技术讲解和原理说明
2. **QUICK_REFERENCE.md** - 快速查找代码位置和使用示例
3. **KNOWLEDGE_GRAPH.md**（本文档）- 理解模块关系和学习路径
4. **PROJECT_DESIGN.md** - 了解业务背景和架构设计
5. **README.md** - 项目介绍和快速开始

---

**建议学习方式**：
1. 先看知识图谱，建立整体认知
2. 按照学习路径，循序渐进
3. 对照快速参考，动手实践
4. 遇到问题，查阅学习指南
5. 定期回顾，巩固知识

**最后更新**：2024-04-15  
**维护者**：EOS Team
