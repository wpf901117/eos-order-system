# Java企业级项目 - 十年经验技术栈综合训练

## 项目概述

### 项目名称
**Enterprise-Order-System** (EOS) - 企业级订单管理系统

### 项目背景
这是一个模拟真实企业场景的订单管理系统，涵盖从需求分析、系统设计到工程落地的完整流程。项目采用微服务架构，使用当前业界主流技术栈，让你通过一个完整的项目，掌握10年Java开发经验应有的技术广度和深度。

### 技术架构图
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              负载均衡层                                     │
│                         (Nginx / SLB)                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              网关层                                          │
│                    (Spring Cloud Gateway)                                   │
│              路由转发 / 限流 / 熔断 / 鉴权                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│   用户服务    │          │   订单服务    │          │   商品服务    │
│ (User Service)│          │(Order Service)│          │(Product Svc)  │
└───────────────┘          └───────────────┘          └───────────────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
            ┌───────────┐   ┌───────────┐   ┌───────────┐
            │  MySQL    │   │  Redis    │   │  Kafka    │
            │ 主从集群  │   │  缓存集群 │   │  消息队列 │
            └───────────┘   └───────────┘   └───────────┘
```

---

## 技术模块与学习路径

### 第一阶段：工程化基础（筑基）

#### 1.1 Maven/Gradle 多模块项目结构
```
enterprise-order-system/
├── eos-parent/                    # 父工程，统一版本管理
│   ├── pom.xml                    # 版本锁定、依赖管理
│   ├── eos-common/               # 公共模块
│   │   ├── eos-common-core/      # 核心工具类
│   │   ├── eos-common-web/       # Web通用配置
│   │   ├── eos-common-redis/     # Redis封装
│   │   └── eos-common-mybatis/   # MyBatis增强
│   ├── eos-api/                  # API定义
│   │   ├── eos-api-user/         # 用户服务API
│   │   ├── eos-api-order/        # 订单服务API
│   │   └── eos-api-product/      # 商品服务API
│   ├── eos-service/              # 服务实现
│   │   ├── eos-service-user/     # 用户服务实现
│   │   ├── eos-service-order/    # 订单服务实现
│   │   └── eos-service-product/  # 商品服务实现
│   └── eos-config/               # 配置文件
```

**核心知识点：**
- Maven BOM管理、dependencyManagement
- Spring Boot Starter自动配置原理
- Spring Boot多模块项目构建
- 统一异常处理、响应封装

#### 1.2 代码质量与规范
- **CheckStyle** 代码风格检查
- **SpotBugs** 潜在Bug检测
- **SonarQube** 代码质量管理
- **Lombok** 简化代码（@Data, @Builder等）
- **MapStruct** 对象映射

### 第二阶段：微服务架构（结丹）

#### 2.1 服务注册与发现
```yaml
# Nacos配置示例
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: ${NACOS_NAMESPACE}
        group: EOS_GROUP
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        refreshable-dataids: common.yaml
```

**核心知识点：**
- CAP定理理解（Raft一致性算法）
- 服务健康检查机制
- 服务权重调整
- 命名空间/分组隔离

#### 2.2 配置中心
```yaml
# 共享配置 common.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

**核心知识点：**
- 配置热更新原理
- 多环境配置切换
- 配置加密（SM4/DES）
- 配置回滚

#### 2.3 API网关
```java
// 自定义过滤器示例
@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private AuthService authService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 权限校验
        // 2. 限流控制
        // 3. 日志记录
        // 4. 请求转发
    }
}
```

**核心知识点：**
- Spring Cloud Gateway工作原理
- 动态路由配置
- 限流算法（令牌桶/滑动窗口）
- 熔断降级（Hystrix/Sentinel）

#### 2.4 负载均衡
```yaml
# Ribbon配置
eos-user-service:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.WeightedResponseTimeRule
    ConnectTimeout: 5000
    ReadTimeout: 3000
    MaxAutoRetries: 2
```

### 第三阶段：数据架构（化神）

#### 3.1 MySQL主从复制
```
┌─────────────┐         同步         ┌─────────────┐
│   Master    │ ─────────────────────▶│   Slave 1   │
│  (写操作)   │                       │  (读操作)   │
└─────────────┘                       └─────────────┘
        │                                   ▲
        │            半同步复制              │
        └───────────────────────────────────┘
```

**核心知识点：**
- binlog/relay-log机制
- GTID复制
- 主从延迟处理
- 读写分离路由（ShardingSphere-JDBC）

#### 3.2 分库分表
```yaml
spring:
  shardingsphere:
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds_${0..1}.t_order_${0..15}
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: order_inline
```

**核心知识点：**
- 分片策略（哈希/范围/时间）
- 分布式ID生成（雪花算法）
- 跨库查询处理
- 分页优化

#### 3.3 Redis缓存架构
```
┌─────────────────────────────────────────┐
│              Redis Cluster              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │Master 1 │ │Master 2 │ │Master 3 │   │
│  │Slave 1-1│ │Slave 2-1│ │Slave 3-1│   │
│  └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────┘
```

**核心知识点：**
- 数据结构：String/Hash/List/Set/ZSet
- 缓存策略：Cache Aside/Read Through/Write Through
- 缓存问题：穿透/击穿/雪崩及解决方案
- 分布式锁：Redisson实现
- Redis 6.x新特性

#### 3.4 MyBatis Plus进阶
```java
// Lambda查询示例
List<OrderVO> orders = orderMapper.selectList(
    new LambdaQueryWrapper<Order>()
        .eq(Order::getStatus, status)
        .ge(Order::getCreateTime, startTime)
        .in(Order::getUserId, userIds)
        .orderByDesc(Order::getCreateTime)
        .last("LIMIT 100")
);
```

**核心知识点：**
- 条件构造器
- 自动填充
- 逻辑删除
- 分页插件原理
- 性能分析

### 第四阶段：消息队列（炼虚）

#### 4.1 Kafka企业级应用
```java
// 订单消息发送
@Service
@Slf4j
public class OrderMessageProducer {

    @Autowired
    private KafkaTemplate<String, OrderMessage> kafkaTemplate;

    /**
     * 发送订单创建消息
     * 采用事务消息确保数据一致性
     */
    public void sendOrderCreated(Order order) {
        // 1. 本地事务处理
        // 2. 发送消息
        // 3. 回调确认
    }
}
```

**核心知识点：**
- 分区与消费者组
- 消息可靠性：ACK机制
- 消息顺序性保证
- 事务消息
- 消息幂等性
- 消息积压处理

#### 4.2 RocketMQ应用
```java
// 延迟消息实现订单超时取消
@RocketMQMessageListener(
    topic = "order延时",
    consumerGroup = "order-timeout-consumer",
   ConsumeMode = ConsumeMode.ORDERLY
)
public class OrderTimeoutListener implements RocketMQListener<OrderTimeoutMsg> {

    @Override
    public void onMessage(OrderTimeoutMsg msg) {
        // 检查订单状态
        // 超时则自动取消
    }
}
```

### 第五阶段：安全架构（合体）

#### 5.1 Spring Security + OAuth2
```java
// JWT Token验证
@Component
public class JwtTokenProvider {

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getAuthorities());
        claims.put("userId", user.getUsername());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expireTime)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
```

**核心知识点：**
- 对称加密/非对称加密
- Token机制（Access/Refresh）
- OAuth2四种授权模式
- 单点登录(SSO)
- 接口幂等性设计

#### 5.2 接口安全
```java
// 接口签名验证
@Component
public class SignValidator {

    /**
     * 参数签名验证
     * 防止请求篡改
     */
    public boolean validateSign(HttpRequest request) {
        String sign = request.getHeader("X-Sign");
        String timestamp = request.getHeader("X-Timestamp");

        // 1. 时间戳校验（5分钟内有效）
        // 2. 签名算法验证
        // 3. 防重放攻击
        return true;
    }
}
```

### 第六阶段：系统设计（渡劫）

#### 6.1 设计模式应用
```java
// 工厂模式 - 支付方式创建
public class PaymentFactory {

    /**
     * 根据支付类型获取支付策略
     * 符合开闭原则，扩展支付方式无需修改此代码
     */
    public static PaymentStrategy getPayment(PaymentType type) {
        return switch (type) {
            case ALIPAY -> new AlipayStrategy();
            case WECHAT -> new WechatPayStrategy();
            case BANK_CARD -> new BankCardStrategy();
        };
    }
}

// 策略模式 - 订单价格计算
public class PriceCalculator {

    public BigDecimal calculate(Order order, MemberLevel level) {
        // 使用策略模式计算不同会员等级的价格
        DiscountStrategy strategy = DiscountStrategyHolder.getStrategy(level);
        return strategy.apply(order);
    }
}
```

#### 6.2 领域驱动设计(DDD)示例
```
订单限界上下文
├── Domain/
│   ├── Entity/         # 订单实体
│   ├── ValueObject/    # 金额、地址
│   ├── Aggregate/       # 订单聚合根
│   ├── Repository/      # 仓储接口
│   └── Service/         # 领域服务
├── Application/
│   ├── Command/         # 命令
│   ├── Query/          # 查询
│   └── Event/          # 领域事件
└── Infrastructure/
    ├── RepositoryImpl/  # 仓储实现
    └── ExternalService/ # 外部服务适配
```

#### 6.3 分布式事务
```java
// Seata AT模式
@GlobalTransactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    // 1. 扣减库存（全局事务保证）
    inventoryService.deductStock(dto.getProductId(), dto.getQuantity());

    // 2. 创建订单
    Order order = orderRepository.save(dto);

    // 3. 扣减余额
    accountService.deductBalance(dto.getUserId(), dto.getAmount());

    // 任一操作失败，全局回滚
}
```

### 第七阶段：性能优化（洞虚）

#### 7.1 JVM调优
```bash
# 生产环境JVM参数
JAVA_OPTS="
  -Xms4g -Xmx4g                    # 堆大小
  -Xmn2g                           # 年轻代
  -XX:MetaspaceSize=512m           # 元空间
  -XX:+UseG1GC                     # G1垃圾收集器
  -XX:MaxGCPauseMillis=200         # 目标停顿时间
  -XX:InitiatingHeapOccupancyPercent=45  # 触发GC时机
  -XX:+HeapDumpOnOutOfMemoryError  # OOM时导出堆
  -XX:HeapDumpPath=/data/logs/     # 堆文件路径
  -XX:+PrintGCDetails              # GC日志
  -Xloggc:/data/logs/gc.log        # GC日志文件
"
```

#### 7.2 SQL优化
```sql
-- 优化前
SELECT * FROM orders WHERE DATE(create_time) = '2024-01-01';

-- 优化后：使用范围查询+覆盖索引
SELECT id, order_no, amount, status
FROM orders
WHERE create_time >= '2024-01-01 00:00:00'
  AND create_time < '2024-01-02 00:00:00';

-- 创建复合索引
CREATE INDEX idx_create_time_status
ON orders(create_time, status);
```

#### 7.3 数据库索引设计
```sql
-- 索引设计原则
-- 1. 主键唯一索引
-- 2. 高频查询字段
-- 3. 区分度高的字段
-- 4. 避免冗余和重复索引

-- 示例：订单表索引
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status_time` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 第八阶段：DevOps与监控（大乘）

#### 8.1 Docker容器化
```dockerfile
# 多阶段构建
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:8-jre-slim
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 8.2 Kubernetes部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eos-order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: eos-order-service
  template:
    spec:
      containers:
      - name: order-service
        image: eos/order-service:latest
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
```

#### 8.3 监控体系
```
┌─────────────────────────────────────────────────────────┐
│                      监控体系                            │
├─────────────────────────────────────────────────────────┤
│  Metrics: Prometheus + Grafana                           │
│  ├─ 基础设施监控 (CPU/内存/磁盘)                         │
│  ├─ JVM监控 (GC/线程/堆内存)                            │
│  ├─ 应用监控 (接口QPS/响应时间/错误率)                    │
│  └─ 业务监控 (订单量/转化率/库存预警)                     │
├─────────────────────────────────────────────────────────┤
│  Logging: ELK Stack                                      │
│  ├─ 日志收集: Filebeat                                   │
│  ├─ 日志存储: Elasticsearch                              │
│  └─ 日志展示: Kibana                                     │
├─────────────────────────────────────────────────────────┤
│  Tracing: SkyWalking / Jaeger                            │
│  └─ 分布式链路追踪                                        │
└─────────────────────────────────────────────────────────┘
```

#### 8.4 APM监控
```java
// SkyWalking埋点示例
@RestController
@RequestMapping("/order")
public class OrderController {

    @GetMapping("/{orderId}")
    @Trace
    public Result<OrderVO> getOrder(@PathVariable Long orderId) {
        // 自动追踪方法调用链
        return orderService.getOrderById(orderId);
    }
}
```

---

## 项目业务场景

### 核心业务流程

#### 场景1：用户下单
```
1. 用户浏览商品 → 商品服务返回商品列表/详情
2. 用户提交订单 → 订单服务创建订单（冻结库存）
3. 支付订单 → 支付服务处理支付
4. 支付成功 → 订单服务确认订单，发送消息
5. 商家发货 → 物流服务处理发货
6. 用户确认收货 → 订单完成
```

#### 场景2：订单超时取消
```
1. 创建订单时发送延迟消息（30分钟后检查）
2. 消费者接收消息
3. 检查订单状态（仍为待支付）
4. 自动取消订单，释放库存
5. 发送取消通知
```

#### 场景3：库存超卖防止
```
1. 乐观锁：UPDATE SET stock = stock - ? WHERE id = ? AND stock >= ?
2. 分布式锁：Redisson lock
3. 消息队列异步扣减
4. 数据库约束：CHECK (stock >= 0)
```

---

## 学习建议

### 学习顺序
1. **第一周**：搭建项目骨架，理解多模块结构
2. **第二周**：实现用户服务，理解Spring Boot核心
3. **第三周**：实现订单服务，理解事务与缓存
4. **第四周**：引入消息队列，理解异步解耦
5. **第五周**：网关与鉴权，理解安全架构
6. **第六周**：性能优化，理解系统调优

### 代码阅读建议
- Spring Boot自动配置源码
- Spring Cloud核心组件原理
- MyBatis Plus执行流程
- Redis分布式锁实现

### 实践建议
1. 每个模块都要自己敲一遍
2. 尝试不同的配置组合
3. 模拟故障场景（服务宕机、网络延迟）
4. 编写单元测试和集成测试

---

## 参考资料

### 书籍
- 《深入理解Java虚拟机》- 周志明
- 《高性能MySQL》- Baron Schwartz
- 《Effective Java》- Joshua Bloch
- 《微服务设计》- Sam Newman

### 技术文档
- Spring Boot官方文档
- Spring Cloud官方文档
- Alibaba Nacos官方文档
- ShardingSphere官方文档

---

## 总结

这个项目涵盖了10年Java开发经验的核心技术点：

| 能力维度 | 技术点 |
|---------|-------|
| 工程化 | Maven多模块、代码规范、质量门禁 |
| 架构能力 | 微服务、分布式、系统设计、DDD |
| 数据层 | MySQL、Redis、读写分离、分库分表 |
| 消息 | Kafka、RocketMQ、事务消息 |
| 安全 | OAuth2、JWT、接口签名、幂等性 |
| 性能 | JVM调优、SQL优化、缓存设计 |
| DevOps | Docker、K8s、监控、日志 |

通过这个项目的学习和实践，你将具备独立负责中等复杂度系统设计和开发的能力，以及与团队协作完成大型系统建设的技术储备。
