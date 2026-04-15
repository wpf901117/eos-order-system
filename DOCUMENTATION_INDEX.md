# EOS 项目文档索引

> 快速找到你需要的文档

---

## 📚 核心文档

### 1. [学习指南 (LEARNING_GUIDE.md)](LEARNING_GUIDE.md)
**适合人群**：所有开发者  
**内容**：全面的技术讲解，包含原理、实现方式、代码示例  
**长度**：约 50 页  

**主要章节**：
- 项目概述与技术架构
- 统一认证模块（JWT + Session）
- 高并发防护体系（幂等性、限流、分布式锁）
- 分布式事务（Seata + RocketMQ）
- DDD 领域驱动设计（充血模型）
- JDK 21 新特性（虚拟线程、Record、Pattern Matching）
- 弹性设计与容错（Sentinel）
- 最佳实践与性能优化
- 分阶段学习路径（初级 → 资深）

**何时阅读**：
- ✅ 系统学习某个技术点
- ✅ 理解实现原理
- ✅ 深入掌握核心技术

---

### 2. [快速参考手册 (QUICK_REFERENCE.md)](QUICK_REFERENCE.md)
**适合人群**：需要快速查找的开发者  
**内容**：速查表、代码位置、使用示例  
**长度**：约 15 页  

**主要内容**：
- 核心功能速查表（认证、高并发、分布式事务等）
- 配置文件速查
- Maven 依赖速查
- 常见场景实现速查
- 问题排查指南
- 性能优化建议
- 部署清单

**何时阅读**：
- ✅ 忘记某个注解怎么用
- ✅ 找不到代码在哪里
- ✅ 需要复制粘贴示例代码
- ✅ 排查常见问题

---

### 3. [技术知识图谱 (KNOWLEDGE_GRAPH.md)](KNOWLEDGE_GRAPH.md)
**适合人群**：想理解模块关系的开发者  
**内容**：可视化架构图、依赖关系、学习路径图  
**长度**：约 20 页  

**主要内容**：
- 技术全景图（从 JDK 21 到 DDD）
- 模块依赖关系图（认证、订单、分布式事务等流程）
- 分阶段学习路径图（10 周学习计划）
- 核心知识点速记
- 推荐阅读顺序

**何时阅读**：
- ✅ 刚开始接触项目，建立整体认知
- ✅ 理解各模块如何协作
- ✅ 规划学习路径
- ✅ 面试前复习

---

### 4. [项目设计文档 (PROJECT_DESIGN.md)](PROJECT_DESIGN.md)
**适合人群**：了解业务背景的开发者  
**内容**：业务场景、架构设计、技术选型理由  
**长度**：约 30 页  

**主要内容**：
- 业务背景与需求分析
- 系统架构设计
- 技术选型对比
- 数据库设计
- API 设计规范
- 部署架构

**何时阅读**：
- ✅ 了解为什么要这样设计
- ✅ 理解业务场景
- ✅ 学习架构思维

---

### 5. [README.md](README.md)
**适合人群**：所有人  
**内容**：项目介绍、快速开始、环境要求  
**长度**：约 5 页

**主要内容**：
- 项目简介
- 技术栈
- 快速开始
- 项目结构
- 贡献指南

**何时阅读**：
- ✅ 第一次接触项目
- ✅ 搭建开发环境
- ✅ 快速运行项目

---

## 🚀 技术模块专项文档

### 6. [监控与可观测性 (MONITORING_GUIDE.md)](MONITORING_GUIDE.md)
**适合人群**：运维工程师、SRE  
**内容**：Actuator + Prometheus + Grafana 监控体系  
**长度**：约 10 页

**主要内容**：
- Actuator 端点配置
- Prometheus 指标采集
- Grafana 仪表盘配置
- 告警规则设置
- 性能监控最佳实践

**何时阅读**：
- ✅ 搭建监控系统
- ✅ 配置告警规则
- ✅ 排查性能问题

---

### 7. [JVM 深度调优指南 (JVM_TUNING_GUIDE.md)](JVM_TUNING_GUIDE.md)
**适合人群**：高级开发者、性能工程师  
**内容**：G1 GC 参数调优、内存分析、问题排查  
**长度**：约 15 页

**主要内容**：
- G1 GC 工作原理
- JVM 参数详解
- GC 日志分析
- 内存泄漏排查
- 性能调优实战
- 生产环境启动脚本

**何时阅读**：
- ✅ JVM 性能调优
- ✅ OOM 问题排查
- ✅ GC 问题分析

---

### 8. [SkyWalking 链路追踪 (SKYWALKING_GUIDE.md)](SKYWALKING_GUIDE.md)
**适合人群**：微服务架构师、SRE  
**内容**：分布式链路追踪实施指南  
**长度**：约 18 页

**主要内容**：
- SkyWalking 架构设计
- Docker Compose 快速部署
- Java Agent 配置
- 自动埋点与手动埋点
- 跨线程追踪
- 日志关联（Trace ID 注入）
- 告警规则配置

**何时阅读**：
- ✅ 搭建链路追踪系统
- ✅ 排查分布式调用问题
- ✅ 性能瓶颈分析

---

### 9. [分库分表实施指南 (SHARDING_GUIDE.md)](SHARDING_GUIDE.md)
**适合人群**：数据库专家、架构师  
**内容**：ShardingSphere-JDBC 读写分离与分库分表  
**长度**：约 15 页

**主要内容**：
- 读写分离配置
- 垂直分库 vs 水平分表
- 分片策略选择
- ShardingSphere 配置详解
- 不分库分表 vs 分库分表对比
- 迁移方案与注意事项

**何时阅读**：
- ✅ 数据库性能优化
- ✅ 海量数据存储设计
- ✅ 读写分离实施

---

### 10. [审计日志与对账系统 (AUDIT_SHARDING_RECONCILIATION_SUMMARY.md)](AUDIT_SHARDING_RECONCILIATION_SUMMARY.md)
**适合人群**：后端开发者、安全工程师  
**内容**：AOP 审计日志、数据对账系统  
**长度**：约 14 页

**主要内容**：
- AOP 切面实现审计日志
- 自定义注解 @AuditLog
- 异步保存审计记录
- 定时对账任务
- 差异检测与告警
- 三种差异类型处理

**何时阅读**：
- ✅ 实现操作审计
- ✅ 数据一致性校验
- ✅ 合规要求（GDPR）

---

### 11. [并发编程实战 (CONCURRENT_CONFIG_SKYWALKING_SUMMARY.md)](CONCURRENT_CONFIG_SKYWALKING_SUMMARY.md)
**适合人群**：中高级开发者  
**内容**：线程池配置、CompletableFuture、多环境管理  
**长度**：约 16 页

**主要内容**：
- ThreadPoolExecutor 参数调优
- CPU 密集型 vs IO 密集型配置
- CompletableFuture 并行查询
- 限流器实现
- 读写锁封装
- 多环境配置管理（dev/test/prod）
- SkyWalking 集成

**何时阅读**：
- ✅ 并发编程学习
- ✅ 性能优化（串行→并行）
- ✅ 多环境部署

---

### 12. [DDD + 设计模式 + 秒杀系统 (DDD_PATTERN_SECKILL_SUMMARY.md)](DDD_PATTERN_SECKILL_SUMMARY.md)
**适合人群**：架构师、高级开发者  
**内容**：领域驱动设计、设计模式实战、高并发秒杀  
**长度**：约 10 页

**主要内容**：
- DDD 核心概念（聚合根、值对象、领域事件）
- 充血模型实现
- 策略模式（多种支付方式）
- 状态机模式（订单状态流转）
- 秒杀系统设计（Redis Lua 脚本）
- 防重复提交与限流

**何时阅读**：
- ✅ DDD 领域建模
- ✅ 设计模式应用
- ✅ 高并发场景设计

---

### 13. [RocketMQ + Redis 高级特性 (ROCKETMQ_REDIS_ADVANCED_SUMMARY.md)](ROCKETMQ_REDIS_ADVANCED_SUMMARY.md)
**适合人群**：中间件专家、架构师  
**内容**：事务消息、顺序消息、分布式锁、缓存一致性  
**长度**：约 11 页

**主要内容**：
- RocketMQ 事务消息（最终一致性）
- RocketMQ 顺序消息（严格顺序保证）
- Redisson 分布式锁（5种锁类型）
- RedLock 算法（多节点高可用）
- 缓存一致性策略（Cache-Aside、Write-Through 等）
- 读写锁提升并发性能

**何时阅读**：
- ✅ 分布式事务实现
- ✅ 消息队列高级应用
- ✅ 缓存一致性保障

---

### 14. [事件驱动架构与 JVM 调优 (EVENT_DRIVEN_AND_JVM_SUMMARY.md)](EVENT_DRIVEN_AND_JVM_SUMMARY.md)
**适合人群**：架构师、性能工程师  
**内容**：领域事件总线、JVM 参数调优、生产启动脚本  
**长度**：约 11 页

**主要内容**：
- 事件驱动架构设计（DomainEventBus）
- 异步事件处理（@Async + @TransactionalEventListener）
- G1 GC 参数详解
- JVM 内存模型
- GC 日志分析
- 内存泄漏排查
- CPU 飙高问题定位
- 生产环境启动脚本（start-production.sh）

**何时阅读**：
- ✅ 实现事件驱动架构
- ✅ JVM 性能调优
- ✅ OOM/CPU 问题排查

---

### 15. [项目工作总结 (WORK_SUMMARY.md)](WORK_SUMMARY.md)
**适合人群**：项目负责人、团队成员  
**内容**：JDK 21 升级、新功能实现、文档整理记录  
**长度**：约 13 页

**主要内容**：
- JDK 版本升级到 21（依赖变更清单）
- Sentinel 弹性设计实现
- DDD 领域驱动设计实现
- JDK 21 新特性应用（虚拟线程、Record、Pattern Matching）
- 文档整理记录
- 学习路径建议

**何时阅读**：
- ✅ 了解项目演进历史
- ✅ 查看技术升级记录
- ✅ 团队工作回顾

---

## 🎯 根据角色选择文档

### 👨‍💻 初级开发者（1-3年经验）

**推荐顺序**：
1. README.md - 了解项目
2. LEARNING_GUIDE.md - 第十章"学习路径" → 初级部分
3. KNOWLEDGE_GRAPH.md - 阶段1学习路径图
4. QUICK_REFERENCE.md - 常用功能速查

**学习目标**：
- 掌握 Spring Boot 基础
- 理解 RESTful API 设计
- 学会使用 MyBatis Plus
- 了解 JWT 认证原理

---

### 👨‍💼 中级开发者（3-5年经验）

**推荐顺序**：
1. LEARNING_GUIDE.md - 第四章"高并发防护"、第五章"分布式事务"
2. KNOWLEDGE_GRAPH.md - 阶段2学习路径图
3. QUICK_REFERENCE.md - 高并发防护速查
4. PROJECT_DESIGN.md - 技术选型理由

**学习目标**：
- 掌握幂等性、限流、分布式锁
- 理解消息队列的使用
- 学会分布式事务方案
- 能够独立设计高并发接口

---

### 👨‍🔬 高级开发者（5-8年经验）

**推荐顺序**：
1. LEARNING_GUIDE.md - 第六章"DDD 领域驱动设计"
2. KNOWLEDGE_GRAPH.md - 阶段3学习路径图
3. PROJECT_DESIGN.md - 架构设计思路
4. QUICK_REFERENCE.md - DDD 实现速查

**学习目标**：
- 掌握 DDD 领域建模
- 理解充血模型 vs 贫血模型
- 学会事件驱动架构
- 能够重构遗留系统

---

### 👨‍🏫 资深开发者/架构师（8-10年+经验）

**推荐顺序**：
1. PROJECT_DESIGN.md - 完整阅读
2. LEARNING_GUIDE.md - 第九章"最佳实践"
3. KNOWLEDGE_GRAPH.md - 技术全景图
4. 所有文档 - 作为团队培训材料

**学习目标**：
- 掌握微服务拆分策略
- 设计完整的监控体系
- 进行全链路压测与调优
- 指导团队技术成长

---

## 🔍 根据场景选择文档

### 场景1：我想学习某个技术点

**例如**：想了解分布式锁怎么实现

**步骤**：
1. 打开 **LEARNING_GUIDE.md**
2. 搜索"分布式锁"或查看目录"四、高并发防护体系 → 4.3 分布式锁"
3. 阅读原理说明和代码示例
4. 打开 **QUICK_REFERENCE.md** 查看快速使用方式
5. 根据文件路径找到实际代码学习

---

### 场景2：我遇到了一个问题

**例如**：Token 验证失败

**步骤**：
1. 打开 **QUICK_REFERENCE.md**
2. 查看"常见问题排查 → 问题1：Token 验证失败"
3. 按照检查项逐一排查
4. 如果未解决，查看 **LEARNING_GUIDE.md** 中"3.1 统一认证模块"的详细原理

---

### 场景3：我要实现一个新功能

**例如**：添加商品评价功能

**步骤**：
1. 打开 **QUICK_REFERENCE.md**
2. 查看"常见场景实现速查"，参考类似功能的实现
3. 打开 **LEARNING_GUIDE.md**
4. 查看"六、DDD 领域驱动设计"，学习如何设计领域模型
5. 参考现有的 Order 聚合根设计 Evaluation 聚合根

---

### 场景4：我要准备面试

**步骤**：
1. 打开 **KNOWLEDGE_GRAPH.md**
2. 查看"核心知识点速记"
3. 重点记忆：
   - JWT vs Session
   - 幂等性实现原理
   - Seata vs RocketMQ 事务
   - DDD 充血模型优势
   - JDK 21 新特性
4. 打开 **LEARNING_GUIDE.md** 深入理解每个知识点

---

### 场景5：我要给团队做培训

**步骤**：
1. 打开 **KNOWLEDGE_GRAPH.md**
2. 使用"技术全景图"作为培训大纲
3. 打开 **LEARNING_GUIDE.md**
4. 每个章节作为一个培训模块
5. 使用 **QUICK_REFERENCE.md** 中的代码示例进行演示
6. 参考"学习路径图"制定培训计划

---

## 📊 文档对比表

| 文档 | 深度 | 广度 | 实用性 | 理论性 | 适合阶段 |
|------|------|------|--------|--------|----------|
| LEARNING_GUIDE.md | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 系统学习 |
| QUICK_REFERENCE.md | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | 快速查阅 |
| KNOWLEDGE_GRAPH.md | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 建立认知 |
| PROJECT_DESIGN.md | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 架构设计 |
| README.md | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ | 快速开始 |
| MONITORING_GUIDE.md | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 运维监控 |
| JVM_TUNING_GUIDE.md | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 性能调优 |
| SKYWALKING_GUIDE.md | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 链路追踪 |
| SHARDING_GUIDE.md | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 数据库优化 |
| AUDIT_SHARDING_RECONCILIATION_SUMMARY.md | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 审计对账 |
| CONCURRENT_CONFIG_SKYWALKING_SUMMARY.md | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 并发编程 |
| DDD_PATTERN_SECKILL_SUMMARY.md | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 架构设计 |
| ROCKETMQ_REDIS_ADVANCED_SUMMARY.md | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 中间件专家 |
| EVENT_DRIVEN_AND_JVM_SUMMARY.md | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 架构+性能 |
| WORK_SUMMARY.md | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 项目回顾 |

---

## 🗺️ 文档关系图

```
                    ┌─────────────────┐
                    │   README.md     │
                    │  (项目入口)      │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ↓              ↓              ↓
    ┌────────────────┐ ┌──────────┐ ┌──────────────┐
    │ LEARNING_      │ │ QUICK_   │ │ KNOWLEDGE_   │
    │ GUIDE.md       │ │ REFERENCE│ │ GRAPH.md     │
    │ (深入学习)      │ │ .md      │ │ (全局认知)    │
    │                │ │ (快速查阅)│ │              │
    └────────┬───────┘ └────┬─────┘ └──────┬───────┘
             │              │               │
             └──────────────┼───────────────┘
                            ↓
                 ┌──────────────────┐
                 │ PROJECT_DESIGN   │
                 │ .md              │
                 │ (架构设计)        │
                 └────────┬─────────┘
                          │
          ┌───────────────┼────────────────┐
          ↓               ↓                ↓
┌────────────────┐ ┌──────────────┐ ┌──────────────┐
│ 监控与可观测性  │ │ JVM 深度调优  │ │ SkyWalking   │
│ MONITORING_    │ │ JVM_TUNING_  │ │ SKYWALKING_  │
│ GUIDE.md       │ │ GUIDE.md     │ │ GUIDE.md     │
└────────────────┘ └──────────────┘ └──────────────┘
          ↓               ↓                ↓
┌────────────────┐ ┌──────────────┐ ┌──────────────┐
│ 分库分表指南    │ │ 审计日志对账  │ │ 并发编程实战  │
│ SHARDING_      │ │ AUDIT_...    │ │ CONCURRENT_  │
│ GUIDE.md       │ │ SUMMARY.md   │ │ SUMMARY.md   │
└────────────────┘ └──────────────┘ └──────────────┘
          ↓               ↓                ↓
┌────────────────┐ ┌──────────────┐ ┌──────────────┐
│ DDD+设计模式   │ │ RocketMQ+    │ │ 事件驱动+    │
│ +秒杀系统      │ │ Redis 高级   │ │ JVM 调优     │
│ DDD_PATTERN_   │ │ ROCKETMQ_    │ │ EVENT_...    │
│ SECKILL_...    │ │ REDIS_...    │ │ SUMMARY.md   │
└────────────────┘ └──────────────┘ └──────────────┘
                                            ↓
                                    ┌──────────────┐
                                    │ 项目工作     │
                                    │ 总结         │
                                    │ WORK_        │
                                    │ SUMMARY.md   │
                                    └──────────────┘
```

---

## 💡 使用建议

### 1. 首次接触项目
```
README.md → KNOWLEDGE_GRAPH.md → LEARNING_GUIDE.md（概览）
```

### 2. 日常开发
```
QUICK_REFERENCE.md（主要）→ LEARNING_GUIDE.md（深入）
```

### 3. 技术提升
```
KNOWLEDGE_GRAPH.md（学习路径）→ LEARNING_GUIDE.md（系统学习）
```

### 4. 问题解决
```
QUICK_REFERENCE.md（问题排查）→ LEARNING_GUIDE.md（原理理解）
```

### 5. 团队协作
```
KNOWLEDGE_GRAPH.md（培训大纲）→ LEARNING_GUIDE.md（培训材料）
```

---

## 📝 文档维护

### 更新频率
- **README.md**: 每次重大变更时更新
- **QUICK_REFERENCE.md**: 每周更新（新增功能时）
- **LEARNING_GUIDE.md**: 每月更新（补充新知识点）
- **KNOWLEDGE_GRAPH.md**: 每季度更新（调整学习路径）
- **PROJECT_DESIGN.md**: 架构调整时更新
- **MONITORING_GUIDE.md**: 监控指标变更时更新
- **JVM_TUNING_GUIDE.md**: JVM 参数优化时更新
- **SKYWALKING_GUIDE.md**: 链路追踪配置变更时更新
- **SHARDING_GUIDE.md**: 分库分表策略调整时更新
- **AUDIT_SHARDING_RECONCILIATION_SUMMARY.md**: 审计规则变更时更新
- **CONCURRENT_CONFIG_SKYWALKING_SUMMARY.md**: 并发配置优化时更新
- **DDD_PATTERN_SECKILL_SUMMARY.md**: DDD 模型重构时更新
- **ROCKETMQ_REDIS_ADVANCED_SUMMARY.md**: 中间件配置变更时更新
- **EVENT_DRIVEN_AND_JVM_SUMMARY.md**: 事件架构调整或 JVM 调优时更新
- **WORK_SUMMARY.md**: 每次重大升级或新功能实现后更新
- **DOCUMENTATION_INDEX.md**: 新增文档时立即更新

### 贡献指南
如果你发现文档中有错误或不清晰的地方，欢迎：
1. 提交 Issue 描述问题
2. 提交 PR 修正文档
3. 补充新的知识点或示例

---

## 🎓 学习资源

### 官方文档
- [Spring Boot 3.2](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [JDK 21](https://openjdk.org/projects/jdk/21/)
- [MyBatis Plus](https://baomidou.com/)
- [Redisson](https://redisson.org/)
- [Sentinel](https://sentinelguard.io/)
- [Seata](https://seata.io/)
- [RocketMQ](https://rocketmq.apache.org/)

### 书籍推荐
- 《深入理解Java虚拟机》- 周志明
- 《领域驱动设计》- Eric Evans
- 《微服务架构设计模式》- Chris Richardson
- 《高性能MySQL》- Baron Schwartz
- 《Redis 设计与实现》- 黄健宏

### 在线课程
- Bilibili: "尚硅谷SpringCloud教程"
- 慕课网: "Java工程师高薪训练营"
- 极客时间: "DDD实战课"

---

## 📞 联系方式

如有问题，欢迎通过以下方式联系：
- GitHub Issues: [提交问题](https://github.com/your-repo/issues)
- 邮件: team@eos-order.com
- 微信群: 扫描项目首页二维码

---

**最后更新**：2024-04-15  
**维护者**：EOS Team  
**文档版本**：v1.0.0
