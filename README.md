# Enterprise Order System (EOS) - 企业级订单管理系统

> 一个专为 Java 高级工程师打造的综合训练项目，涵盖微服务架构、分布式系统、性能优化、DevOps 等十年开发经验应有的核心技术栈。

---

## 📚 文档导航

为了帮助你更高效地学习本项目，我们准备了以下文档：

| 文档 | 说明 | 适合人群 |
|------|------|----------|
| [📘 学习指南](LEARNING_GUIDE.md) | 全面的技术讲解，包含原理、实现方式、代码示例 | 所有开发者 |
| [📙 快速参考手册](QUICK_REFERENCE.md) | 速查表、代码位置、使用示例 | 需要快速查找的开发者 |
| [🗺️ 技术知识图谱](KNOWLEDGE_GRAPH.md) | 可视化架构图、依赖关系、学习路径图 | 想理解模块关系的开发者 |
| [📋 文档索引](DOCUMENTATION_INDEX.md) | 快速找到你需要的文档 | 所有人 |
| [📐 项目设计文档](PROJECT_DESIGN.md) | 业务场景、架构设计、技术选型理由 | 了解业务背景的开发者 |

**💡 建议阅读顺序**：
1. 新手：README → KNOWLEDGE_GRAPH → LEARNING_GUIDE
2. 日常开发：QUICK_REFERENCE（主要）→ LEARNING_GUIDE（深入）
3. 技术提升：KNOWLEDGE_GRAPH（学习路径）→ LEARNING_GUIDE（系统学习）
4. 问题解决：QUICK_REFERENCE（问题排查）→ LEARNING_GUIDE（原理理解）

详细的选择指南请查看：[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)

---

## 项目概述

### 为什么做这个项目？

如果你已经做了几年 CRUD，但感觉自己始终停留在"写业务逻辑"的层面，缺少系统设计和架构能力，这个项目就是为你量身定制的。

通过一个**完整的、可运行的、贴近生产环境**的订单系统，你将系统性地掌握：

- 如何设计一个企业级的 Maven 多模块项目
- 如何用 Spring Cloud Alibaba 搭建真正的微服务架构
- 如何处理分布式事务、缓存一致性、并发安全等"硬骨头"问题
- 如何写出高质量的代码（设计模式、DDD、Clean Code）
- 如何进行性能优化和系统调优
- 如何用 Docker、K8s、监控体系完成工程化落地

### 技术架构

```
                    ┌─────────────┐
                    │   Nginx     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Gateway   │  ← 统一入口：路由、鉴权、限流
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │  User   │       │ Product │       │  Order  │
   │ Service │       │ Service │       │ Service │
   │ :8081   │       │ :8082   │       │ :8083   │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         ┌────▼────┐  ┌───▼────┐  ┌────▼────┐
         │  MySQL  │  │ Redis  │  │ RocketMQ│
         └─────────┘  └────────┘  └─────────┘
```

---

## 目录结构

```
enterprise-order-system/
├── pom.xml                                    # 父POM，统一依赖版本
├── README.md                                  # 项目说明
├── PROJECT_DESIGN.md                          # 详细技术设计文档
├── docker-compose.yml                         # 一键启动中间件
├── scripts/
│   └── init.sql                               # 数据库初始化脚本
├── eos-common/                                # 公共模块
│   ├── eos-common-core/                       # 核心工具、统一响应、异常处理
│   ├── eos-common-mybatis/                    # MyBatis Plus 配置
│   └── eos-common-redis/                      # Redis 配置
├── eos-service/                               # 业务服务
│   ├── eos-service-user/                      # 用户服务
│   ├── eos-service-product/                   # 商品服务
│   └── eos-service-order/                     # 订单服务
└── eos-gateway/                               # API网关
```

---

## 快速开始

### 环境准备

- **JDK 21+** （已升级至 JDK 21 LTS，支持虚拟线程等新特性）
- **Maven 3.8+**
- **Docker & Docker Compose**（可选，用于启动中间件）
- **MySQL 8.3+**（如不使用 Docker）
- **Redis 7.x+**
- **Nacos 2.x+**
- **RocketMQ 5.x+**
- **Seata Server**（可选，用于分布式事务）

### 方式一：Docker Compose 一键启动

```bash
# 1. 启动所有中间件
docker-compose up -d

# 2. 初始化数据库（MySQL启动后执行）
mysql -h127.0.0.1 -uroot -proot < scripts/init.sql
```

### 方式二：手动安装中间件

如果你本地已有 MySQL/Redis/Nacos/RocketMQ，直接修改各服务的 `application.yml` 连接地址即可。

### 编译运行

```bash
# 1. 编译整个项目
mvn clean install -DskipTests

# 2. 启动各服务（建议按顺序启动）
# 2.1 启动网关
cd eos-gateway && mvn spring-boot:run
# 2.2 启动用户服务
cd eos-service/eos-service-user && mvn spring-boot:run
# 2.3 启动商品服务
cd eos-service/eos-service-product && mvn spring-boot:run
# 2.4 启动订单服务
cd eos-service/eos-service-order && mvn spring-boot:run
```

---

## 接口测试

### 1. 用户注册

```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "phone": "13800138000"
  }'
```

### 2. 用户登录

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

> **响应说明**：登录成功后返回 `accessToken`（访问令牌，有效期30分钟）和 `refreshToken`（刷新令牌，有效期7天）。前端应将两者都保存下来，当 access token 即将过期时，用 refresh token 换取新的 access token。

### 3. 刷新访问令牌

```bash
curl -X POST http://localhost:8080/user/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

> **滑动过期**：每次刷新成功后，Session 在 Redis 中的 TTL 会重置为7天，实现用户持续活跃则永不过期的体验。

### 4. 查询商品列表（无需登录）

```bash
curl http://localhost:8080/product/list
```

### 5. 创建订单（需要Token）

```bash
curl -X POST http://localhost:8080/order/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "productId": 2000000000000000001,
    "quantity": 1,
    "address": "北京市海淀区xxx",
    "totalAmount": 7999.00
  }'
```

### 5. 查询我的订单（需要Token）

```bash
curl "http://localhost:8080/order/list?pageNo=1&pageSize=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6. 支付订单

```bash
curl -X POST http://localhost:8080/order/ORDER_ID/pay \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 7. 管理员发货

```bash
curl -X POST http://localhost:8080/order/ORDER_ID/ship \
  -H "Authorization: Bearer ADMIN_ACCESS_TOKEN"
```

### 8. 确认收货

```bash
curl -X POST http://localhost:8080/order/ORDER_ID/confirm \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 9. 管理员分页查询商品

```bash
curl "http://localhost:8080/product/admin/page?pageNo=1&pageSize=10&status=1" \
  -H "Authorization: Bearer ADMIN_ACCESS_TOKEN"
```

### 10. 管理员新增商品

```bash
curl -X POST http://localhost:8080/product/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_ACCESS_TOKEN" \
  -d '{
    "name": "iPad Pro 13",
    "description": "M4 芯片平板电脑",
    "price": 8999.00,
    "stock": 30,
    "imageUrl": "https://example.com/ipad.jpg",
    "categoryId": 1001
  }'
```

### 11. 用户登出

```bash
curl -X POST http://localhost:8080/user/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

> 登出后当前 session 会被撤销，旧的 access token 和 refresh token 都无法继续使用。

---

## 核心技术点详解

### 🚀 JDK 21 新特性应用

- **虚拟线程（Virtual Threads）**：轻量级线程，支持百万并发，适合 I/O 密集型任务
- **Record 类**：不可变数据载体，自动生成 constructor、equals、hashCode、toString
- **Pattern Matching for Switch**：更简洁的条件判断，支持 null 安全处理和 Guarded Patterns
- **Switch 表达式**：编译时检查 exhaustiveness，避免遗漏分支

### 1. Maven 多模块工程化

- 父 POM 用 `dependencyManagement` 统一锁定所有中间件版本
- 子模块按需引用，避免依赖冲突
- 代码中做了详细的版本兼容性说明

### 2. Spring Cloud Alibaba 微服务

- **Nacos**：服务注册发现 + 配置中心
- **Gateway**：统一入口，动态路由
- **OpenFeign**：声明式远程调用 + Fallback 降级
- **Sentinel**：限流熔断（网关层已集成）

### 3. 安全架构

- **Spring Security + BCrypt**：密码安全存储
- **双 Token + 滑动过期**：
  - Access Token（有效期30分钟）：携带用户身份访问业务接口
  - Refresh Token（有效期7天）：用于换取新的 Access Token
  - Redis Session：持久化会话状态，支持会话撤销（登出生效）
  - 滑动过期：每次刷新后 Session TTL 重置为7天，持续活跃则永不过期
- **网关统一鉴权**：Gateway 拦截所有请求，验证 Token + Session，拒绝 Refresh Token 访问业务接口
- **Redis 黑名单**：Access Token 黑名单兜底，配合 Session 撤销实现完整登出
- **接口幂等性**：防止重复提交订单

### 4. 数据层设计

- **MyBatis Plus**：ORM 增强，自动分页、自动填充、逻辑删除
- **Druid 连接池**：SQL 监控、防注入
- **数据库索引**：复合索引、覆盖索引优化实战
- **雪花算法**：分布式 ID 生成，解决分库分表 ID 冲突

### 5. 缓存与并发

- **Redis + Spring Cache**：@Cacheable / @CacheEvict 注解式缓存
- **Redisson 分布式锁**：防止库存超卖
- **乐观锁**：MySQL `WHERE stock >= quantity` 原子扣减
- **缓存一致性**：库存变更时同步清除缓存和更新 Redis

### 6. 消息队列

- **RocketMQ 延迟消息**：订单 30 分钟未支付自动取消
- **事务消息**：确保本地事务和消息发送一致性
- **消费者幂等性**：同一消息不会被重复处理

### 7. 分布式事务

- **Seata AT 模式**：订单创建 + 库存扣减跨服务事务一致性
- `@GlobalTransactional` 注解实现开箱即用

### 8. DDD 领域驱动设计

- **充血模型**：业务逻辑封装在领域对象内部，而非 Service 层
- **聚合根（Order）**：保证聚合内的一致性，外部唯一入口
- **值对象（Money、OrderStatus）**：不可变性，通过值相等性判断
- **领域事件**：解耦业务流程，支持异步处理
- **应用服务**：只负责编排，不包含业务逻辑

### 9. 设计模式应用

- **工厂模式**：PaymentFactory（支付方式创建）
- **策略模式**：DiscountStrategy（价格计算策略）
- **模板方法模式**：Service 层抽象公共流程

### 10. 性能优化

- **JVM 参数调优**：G1 垃圾收集器、堆内存配置
- **SQL 优化**：避免全表扫描、使用覆盖索引
- **连接池优化**：Druid 连接数、超时时间配置

### 11. DevOps

- **Dockerfile**：多阶段构建，减小镜像体积
- **Docker Compose**：一键启动全部中间件
- **K8s YAML**：Deployment + Service 部署模板

---

## 学习路径建议

### 第一周：打基础

1. 通读 `PROJECT_DESIGN.md`，理解整体架构
2. 运行 `docker-compose up -d` 启动中间件
3. 执行 `init.sql` 初始化数据库
4. 成功启动所有服务，用 Postman/curl 调通接口

### 第二周：深入业务

1. 逐行阅读 `UserServiceImpl`，理解注册登录流程
2. 阅读 `ProductServiceImpl`，重点看缓存和分布式锁
3. 阅读 `OrderServiceImpl`，重点看分布式事务和延迟消息
4. 动手修改代码，比如调整延迟消息时间、修改缓存过期时间

### 第三周：原理深挖

1. 打开 Spring Boot 自动配置源码，理解 `spring.factories`
2. 阅读 MyBatis Plus 的 `PaginationInterceptor` 源码
3. 阅读 Redisson 的 `RLock` 实现原理
4. 学习 Seata AT 模式的 UndoLog 机制

### 第四周：性能与调优

1. 用 JMeter 压测下单接口，观察 QPS 和错误率
2. 开启 MySQL 慢查询日志，分析是否有慢 SQL
3. 用 `jvisualvm` 或 `arthas` 分析 JVM 内存和线程
4. 尝试给热点接口加本地缓存（Caffeine）

---

## 常见问题

### Q1: Nacos 连接不上怎么办？

确保 Nacos 已经启动，并且 `application.yml` 中的 `server-addr` 配置正确。如果是 Docker 启动，确认宿主机可以访问容器端口。

### Q2: RocketMQ 延迟消息没生效？

RocketMQ 的延迟级别是预设的，不支持任意时间。请参考 `OrderTimeoutProducer` 中的 `mapDelayLevel` 方法。

### Q3: Seata 分布式事务报错？

确保每个业务数据库都创建了 `undo_log` 表，并且 Seata Server 已经启动。

### Q4: 如何添加新的微服务？

1. 在 `eos-service` 下新建 Maven 模块
2. 继承 `eos-service` 父 POM
3. 引入需要的公共模块（如 `eos-common-core`）
4. 在 `application.yml` 中配置服务名和 Nacos 地址
5. 在 `eos-gateway` 的 `application.yml` 中添加路由规则

---

## 扩展方向

如果你已经吃透了基础代码，可以尝试以下扩展：

1. **支付模块**：接入支付宝/微信支付沙箱环境
2. **搜索模块**：引入 Elasticsearch 实现商品搜索
3. **推荐模块**：用 Redis 的 ZSet 实现热门商品排行榜
4. **秒杀模块**：实现 Redis 预扣库存 + 消息队列异步下单
5. **监控告警**：接入 Prometheus + Grafana + SkyWalking
6. **CI/CD**：编写 Jenkinsfile 或 GitHub Actions 流水线

---

## 参考资料

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Alibaba](https://sca.aliyun.com/)
- [MyBatis Plus](https://baomidou.com/)
- [RocketMQ 官方文档](https://rocketmq.apache.org/)
- [Seata 官方文档](https://seata.io/)
- 《深入理解Java虚拟机》- 周志明
- 《高性能MySQL》- Baron Schwartz
- 《微服务设计》- Sam Newman

---

## 版权说明

本项目仅供学习交流使用，代码中的设计思路和最佳实践参考了阿里巴巴 Java 开发手册、Spring 官方文档以及业界主流微服务架构方案。

如有问题或建议，欢迎交流探讨。

---

**祝你学习愉快，早日突破 CRUD 的瓶颈！**
