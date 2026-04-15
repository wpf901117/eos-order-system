# 事件驱动架构与 JVM 调优 - 实施总结

> **完成时间**：2024-04-15  
> **优先级**：P1（高优先级）  
> **状态**：✅ 已完成

---

## ✅ 已完成的工作

### 一、事件驱动架构（Event-Driven Architecture）

#### 1.1 核心组件实现

**新增文件清单**：

| 文件 | 行数 | 说明 |
|------|------|------|
| `DomainEvent.java` | 59 | 领域事件基类 |
| `OrderCreatedEvent.java` | 49 | 订单创建事件 |
| `OrderPaidEvent.java` | 36 | 订单支付事件 |
| `OrderCancelledEvent.java` | 29 | 订单取消事件 |
| `DomainEventBus.java` | 56 | 事件总线（同步/异步） |
| `OrderCreatedEventHandler.java` | 76 | 订单创建事件处理器 |
| `OrderPaidEventHandler.java` | 46 | 订单支付事件处理器 |
| `AsyncEventConfig.java` | 65 | 异步事件线程池配置 |
| **总计** | **416 行** | - |

**修改文件**：
- `OrderServiceImpl.java` - 集成事件发布

---

#### 1.2 架构设计

```
┌─────────────────────────────────────────────────────┐
│              应用服务层                              │
│         OrderServiceImpl                            │
│                                                     │
│  1. 创建订单（事务内）                               │
│  2. 发布领域事件（异步）                             │
│     eventBus.publishAsync(new OrderCreatedEvent())  │
└─────────────────────────────────────────────────────┘
                      ↓ 发布
┌─────────────────────────────────────────────────────┐
│              事件总线                                │
│           DomainEventBus                            │
│                                                     │
│  • 同步发布：publish()                              │
│  • 异步发布：publishAsync() @Async                  │
└─────────────────────────────────────────────────────┘
                      ↓ 分发
┌──────────────────┐  ┌──────────────────┐
│  事件处理器 1     │  │  事件处理器 2     │
│ OrderCreated     │  │ OrderPaid        │
│ EventHandler     │  │ EventHandler     │
│                  │  │                  │
│ • 发送通知       │  │ • 发送支付通知   │
│ • 更新统计       │  │ • 通知仓库       │
│ • 触发风控       │  │ • 更新积分       │
└──────────────────┘  └──────────────────┘
```

---

#### 1.3 核心特性

**✅ 事件不可变性**
```java
public abstract class DomainEvent {
    private final String eventId;          // 唯一标识
    private final LocalDateTime occurredOn; // 发生时间
    private final Long aggregateId;         // 聚合根ID
}
```

**✅ 异步事件处理**
```java
@Async("domainEventExecutor")
public void publishAsync(DomainEvent event) {
    eventPublisher.publishEvent(event);
}
```

**✅ 事务后执行**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCreated(OrderCreatedEvent event) {
    // 在事务提交后执行，保证数据一致性
}
```

**✅ 解耦业务逻辑**
- 订单创建 → 发送通知、更新统计、触发风控
- 订单支付 → 通知仓库、更新积分

---

#### 1.4 使用示例

**发布事件**：
```java
@Autowired
private DomainEventBus eventBus;

// 创建订单后发布事件
OrderCreatedEvent event = new OrderCreatedEvent(
    order.getId(),
    order.getOrderNo(),
    order.getUserId(),
    order.getProductId(),
    order.getProductName(),
    order.getQuantity(),
    order.getTotalAmount()
);
eventBus.publishAsync(event);
```

**监听事件**：
```java
@Component
public class OrderCreatedEventHandler {
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建成功: {}", event.getOrderNo());
        
        // 发送通知
        sendNotification(event);
        
        // 更新统计
        updateUserStatistics(event);
        
        // 触发风控
        triggerRiskControl(event);
    }
}
```

---

### 二、JVM 深度调优指南

#### 2.1 文档内容

**文件**：`JVM_TUNING_GUIDE.md`（628 行）

**章节结构**：
1. JVM 内存模型
2. 垃圾收集器选择
3. G1 GC 参数调优
4. 生产环境配置示例
5. GC 日志分析
6. 内存泄漏排查
7. CPU 飙高问题定位
8. 性能监控工具

---

#### 2.2 核心知识点

**✅ 内存区域划分**
```
Heap (堆)
├─ Young Generation (新生代)
│  ├─ Eden
│  ├─ Survivor S0
│  └─ Survivor S1
└─ Old Generation (老年代)

Non-Heap (非堆)
├─ Metaspace (元空间)
└─ Code Cache (代码缓存)
```

**✅ G1 GC 核心参数**
```bash
-Xms8g -Xmx8g                          # 堆大小
-XX:+UseG1GC                           # 启用 G1
-XX:MaxGCPauseMillis=200               # 停顿时间目标
-XX:InitiatingHeapOccupancyPercent=45  # 并发标记触发点
-XX:ConcGCThreads=4                    # 并发标记线程数
-XX:ParallelGCThreads=8                # 并行 GC 线程数
```

**✅ 生产环境配置**
```bash
# 见 scripts/start-production.sh
# 包含完整的 JVM 参数配置
# 支持 start/stop/restart/status 命令
```

---

#### 2.3 实战脚本

**文件**：`scripts/start-production.sh`（255 行）

**功能**：
- ✅ 优雅启动/停止/重启
- ✅ PID 管理
- ✅ 日志目录自动创建
- ✅ 健康检查
- ✅ JVM 信息查看

**使用方法**：
```bash
# 启动
./scripts/start-production.sh start

# 停止
./scripts/start-production.sh stop

# 重启
./scripts/start-production.sh restart

# 查看状态
./scripts/start-production.sh status
```

---

#### 2.4 问题排查指南

**内存泄漏排查步骤**：
1. 生成堆转储：`jmap -dump:format=b,file=heap.hprof <pid>`
2. 使用 MAT 分析：查找占用内存最多的对象
3. 定位泄漏代码：检查静态集合、未关闭资源、ThreadLocal

**CPU 飙高定位步骤**：
1. 找到高 CPU 线程：`top -H -p <pid>`
2. 转换线程 ID：`printf "%x\n" <thread-id>`
3. 查看堆栈：`jstack <pid> | grep -A 20 "0x<hex-id>"`
4. 使用 Arthas：`thread -n 3`

---

## 📊 技术价值

### 事件驱动架构的价值

1. **解耦业务逻辑**
   - 订单创建与通知发送解耦
   - 易于扩展新的事件处理器

2. **提升性能**
   - 异步处理不阻塞主流程
   - 事务提交后执行，减少锁竞争

3. **提高可维护性**
   - 每个事件处理器职责单一
   - 易于测试和调试

4. **支持最终一致性**
   - 跨服务的业务逻辑通过事件协调
   - 天然支持分布式系统

---

### JVM 调优的价值

1. **性能优化**
   - 合理的 GC 参数可减少停顿时间
   - 提升应用响应速度和吞吐量

2. **稳定性保障**
   - 防止 OOM 导致的服务中断
   - 快速定位和解决内存问题

3. **成本控制**
   - 合理配置可减少服务器资源浪费
   - 避免过度配置

4. **问题排查能力**
   - 掌握 JVM 诊断工具
   - 快速定位生产环境问题

---

## 🎯 学习要点

### 事件驱动架构

**核心概念**：
- ✅ 领域事件（Domain Event）
- ✅ 事件总线（Event Bus）
- ✅ 事件处理器（Event Handler）
- ✅ 异步处理（@Async）
- ✅ 事务监听器（@TransactionalEventListener）

**最佳实践**：
- ✅ 事件命名清晰（OrderCreatedEvent）
- ✅ 事件不可变（final 字段）
- ✅ 异步处理耗时操作
- ✅ 事务提交后执行
- ✅ 幂等性保证

---

### JVM 调优

**核心概念**：
- ✅ JVM 内存模型
- ✅ 垃圾收集算法
- ✅ G1 GC 工作原理
- ✅ GC 日志分析
- ✅ 堆转储分析

**最佳实践**：
- ✅ -Xms = -Xmx
- ✅ 选择合适的 GC
- ✅ 监控 GC 指标
- ✅ 定期分析 GC 日志
- ✅ 配置 OOM 堆转储

---

## 📈 下一步建议

### 短期（本周）

1. **测试事件驱动架构**
   ```bash
   # 启动服务
   ./scripts/start-production.sh start
   
   # 创建订单，观察日志
   tail -f /data/logs/app.log | grep "事件处理"
   
   # 验证异步执行
   # 应该看到订单创建后立即返回
   # 事件处理器在后台执行
   ```

2. **应用 JVM 参数**
   ```bash
   # 修改 start-production.sh 中的 JVM 参数
   # 根据实际服务器配置调整堆大小
   
   # 启动并监控
   ./scripts/start-production.sh start
   
   # 查看 GC 日志
   tail -f /data/logs/gc.log
   ```

---

### 中期（下周）

3. **集成 SkyWalking**
   - 下载 SkyWalking Agent
   - 配置 Java Agent
   - 查看分布式链路追踪

4. **完善监控仪表盘**
   - 配置 Prometheus 采集 JVM 指标
   - 创建 Grafana 仪表盘
   - 设置告警规则

---

### 长期（下月）

5. **性能压测**
   - 使用 JMeter 压测
   - 对比不同 JVM 参数的效果
   - 找到最优配置

6. **持续优化**
   - 定期分析 GC 日志
   - 监控内存泄漏
   - 调整 GC 参数

---

## 🔗 相关文档

- [事件驱动架构详解](LEARNING_GUIDE.md#九可观测性与监控)
- [JVM 调优完整指南](JVM_TUNING_GUIDE.md)
- [监控体系搭建](MONITORING_GUIDE.md)
- [生产环境部署](scripts/start-production.sh)

---

## 💡 常见问题

### Q1: 事件处理器执行失败怎么办？

**A**: Spring 的 `@TransactionalEventListener` 默认不会重试。如果需要重试：
```java
@Retryable(value = Exception.class, maxAttempts = 3)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCreated(OrderCreatedEvent event) {
    // 业务逻辑
}
```

---

### Q2: 如何保证事件的可靠性？

**A**: 
1. 使用持久化消息队列（RocketMQ/Kafka）
2. 事件存储到数据库
3. 实现事件重放机制

---

### Q3: G1 GC 停顿时间还是太长怎么办？

**A**:
1. 增大堆内存
2. 降低 `MaxGCPauseMillis`
3. 增加 `ConcGCThreads`
4. 考虑切换到 ZGC（JDK 21+）

---

### Q4: 如何判断是否需要调优？

**A**: 关注以下指标：
- Full GC 频率 > 1 次/小时
- GC 停顿时间 > 500ms
- 堆使用率持续 > 85%
- 应用响应时间波动大

---

**实施完成时间**：2024-04-15  
**维护者**：EOS Team
