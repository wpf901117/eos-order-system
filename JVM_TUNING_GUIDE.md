# JVM 深度调优指南

> **适用版本**：JDK 21  
> **目标**：生产环境性能优化与问题排查  
> **难度**：高级（需要 JVM 基础知识）

---

## 📚 目录

- [一、JVM 内存模型](#一jvm-内存模型)
- [二、垃圾收集器选择](#二垃圾收集器选择)
- [三、G1 GC 参数调优](#三g1-gc-参数调优)
- [四、生产环境配置示例](#四生产环境配置示例)
- [五、GC 日志分析](#五gc-日志分析)
- [六、内存泄漏排查](#六内存泄漏排查)
- [七、CPU 飙高问题定位](#七cpu-飙高问题定位)
- [八、性能监控工具](#八性能监控工具)

---

## 一、JVM 内存模型

### 1.1 内存区域划分

```
┌─────────────────────────────────────────┐
│              Heap (堆)                   │
│  ┌──────────────┐  ┌────────────────┐  │
│  │ Young Gen    │  │ Old Gen        │  │
│  │ (新生代)      │  │ (老年代)        │  │
│  │              │  │                │  │
│  │ Eden         │  │                │  │
│  │ Survivor S0  │  │                │  │
│  │ Survivor S1  │  │                │  │
│  └──────────────┘  └────────────────┘  │
├─────────────────────────────────────────┤
│          Metaspace (元空间)             │
│          (类元数据、常量池)              │
├─────────────────────────────────────────┤
│          Code Cache (代码缓存)          │
│          (JIT 编译后的本地代码)          │
└─────────────────────────────────────────┘
```

### 1.2 各区域作用

| 区域 | 说明 | 调优重点 |
|------|------|---------|
| **Eden** | 新对象分配区 | 大小影响 Minor GC 频率 |
| **Survivor** | 存活对象过渡区 | S0/S1 比例影响对象晋升 |
| **Old Gen** | 长期存活对象 | 大小影响 Full GC 频率 |
| **Metaspace** | 类元数据 | 防止元空间溢出 |
| **Code Cache** | JIT 编译代码 | 影响编译优化效果 |

---

## 二、垃圾收集器选择

### 2.1 JDK 21 可用收集器

| 收集器 | 类型 | 适用场景 | 特点 |
|--------|------|---------|------|
| **G1** | 分代/分区 | 通用场景（推荐） | 可预测停顿时间 |
| **ZGC** | 不分代 | 超低延迟（<10ms） | JDK 21 成熟稳定 |
| **Serial** | 单线程 | 客户端应用 | 简单但停顿长 |
| **Parallel** | 多线程 | 吞吐量优先 | 停顿时间不可控 |

### 2.2 推荐选择

**生产环境推荐**：
- ✅ **G1 GC**：大多数场景（堆 < 32GB）
- ✅ **ZGC**：超低延迟要求（堆 > 32GB，JDK 21+）

**本项目的选择**：**G1 GC**

**理由**：
- 订单服务堆内存预计 8-16GB
- G1 在中等堆大小下表现优秀
- 可预测的停顿时间（目标 < 200ms）
- 成熟的生产和监控经验

---

## 三、G1 GC 参数调优

### 3.1 核心参数

#### 堆内存配置

```bash
# 初始堆大小 = 最大堆大小（避免动态调整开销）
-Xms8g
-Xmx8g

# 新生代大小（可选，G1 会自动调整）
-Xmn2g
```

**原则**：
- `-Xms` 和 `-Xmx` 设置为相同值
- 堆大小不超过物理内存的 75%
- 预留足够内存给操作系统和其他进程

---

#### G1 专用参数

```bash
# 启用 G1 收集器（JDK 9+ 默认）
-XX:+UseG1GC

# 最大 GC 停顿时间目标（毫秒）
-XX:MaxGCPauseMillis=200

# 触发并发标记的堆占用百分比
-XX:InitiatingHeapOccupancyPercent=45

# 并发标记线程数（默认为 CPU 核心数的 1/4）
-XX:ConcGCThreads=4

# 并行 GC 线程数（默认为 CPU 核心数）
-XX:ParallelGCThreads=8

# 年轻代最小占比
-XX:G1HeapRegionSize=16m
```

**参数解释**：

**MaxGCPauseMillis**：
- 目标：GC 停顿时间不超过此值
- 默认：200ms
- 调整：根据业务容忍度设置（100-500ms）
- 注意：过小会导致频繁 GC，过大会增加停顿

**InitiatingHeapOccupancyPercent**：
- 含义：堆使用率达到此百分比时触发并发标记
- 默认：45%
- 调整：
  - 降低（35-40%）：更早触发标记，减少 Full GC
  - 提高（50-60%）：减少标记频率，但可能增加 Full GC 风险

**ConcGCThreads**：
- 含义：并发标记阶段使用的线程数
- 默认：CPU 核心数 / 4
- 调整：增加可加快标记速度，但会占用更多 CPU

---

#### 元空间配置

```bash
# 初始元空间大小
-XX:MetaspaceSize=256m

# 最大元空间大小（不限制可能导致 OOM）
-XX:MaxMetaspaceSize=512m
```

**原则**：
- 根据应用中加载的类数量设置
- Spring Boot 应用通常需要 256-512MB
- 设置 MaxMetaspaceSize 防止内存泄漏

---

#### 其他重要参数

```bash
# 启用并行引用处理
-XX:+ParallelRefProcEnabled

# GC 日志配置（JDK 21 统一日志）
-Xlog:gc*:file=/data/logs/gc.log:time,uptime:filecount=5,filesize=100M

# OOM 时生成堆转储文件
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump.hprof

# 错误日志
-XX:ErrorFile=/data/logs/hs_err_pid%p.log
```

---

### 3.2 调优步骤

#### 步骤1：基准测试

```bash
# 使用默认参数运行应用
java -jar app.jar

# 压测并记录指标
# - QPS
# - 平均响应时间
# - P99 响应时间
# - GC 次数和停顿时间
```

#### 步骤2：调整堆大小

```bash
# 观察 GC 日志，判断是否需要调整
# 如果 Young GC 频繁 → 增大新生代
# 如果 Full GC 频繁 → 增大老年代或整个堆
```

#### 步骤3：调整停顿时间目标

```bash
# 如果实际停顿时间 > 目标值
# 1. 增大 MaxGCPauseMillis
# 2. 或增大堆内存
# 3. 或增加 ConcGCThreads
```

#### 步骤4：验证效果

```bash
# 再次压测，对比指标
# 确保：
# - QPS 提升或持平
# - 响应时间降低
# - GC 停顿时间在可接受范围
```

---

## 四、生产环境配置示例

### 4.1 订单服务（8GB 堆）

**文件**：`scripts/start-order-service.sh`

```bash
#!/bin/bash

APP_NAME="eos-order-service"
APP_JAR="eos-service-order-1.0.0-SNAPSHOT.jar"
LOG_DIR="/data/logs"

# JVM 参数
JVM_OPTS="
# 堆内存
-Xms8g
-Xmx8g

# G1 GC
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:ConcGCThreads=4
-XX:ParallelGCThreads=8
-XX:+ParallelRefProcEnabled

# 元空间
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# GC 日志
-Xlog:gc*:file=${LOG_DIR}/gc.log:time,uptime:filecount=5,filesize=100M

# 堆转储
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof

# 错误日志
-XX:ErrorFile=${LOG_DIR}/hs_err_pid%p.log

# 其他优化
-XX:+UnlockExperimentalVMOptions
-XX:+UseContainerSupport
-Djava.security.egd=file:/dev/./urandom
"

# 启动应用
nohup java $JVM_OPTS -jar $APP_JAR > ${LOG_DIR}/app.log 2>&1 &

echo "$APP_NAME started with PID $!"
```

---

### 4.2 用户服务（4GB 堆）

```bash
JVM_OPTS="
-Xms4g
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=150
-XX:InitiatingHeapOccupancyPercent=40
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-Xlog:gc*:file=/data/logs/user-gc.log:time,uptime:filecount=5,filesize=100M
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/user-heapdump.hprof
"
```

---

### 4.3 网关服务（2GB 堆）

```bash
JVM_OPTS="
-Xms2g
-Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:InitiatingHeapOccupancyPercent=35
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-Xlog:gc*:file=/data/logs/gateway-gc.log:time,uptime:filecount=5,filesize=100M
"
```

---

## 五、GC 日志分析

### 5.1 查看 GC 日志

```bash
# 实时查看 GC 日志
tail -f /data/logs/gc.log

# 统计 GC 次数
grep "Pause Young" /data/logs/gc.log | wc -l

# 统计 Full GC 次数
grep "Pause Full" /data/logs/gc.log | wc -l

# 查看最长停顿时间
grep "Pause" /data/logs/gc.log | awk '{print $NF}' | sort -n | tail -1
```

---

### 5.2 GC 日志示例

```
[2024-04-15T10:30:15.123+0800][info][gc] GC(123) Pause Young (Normal) (G1 Evacuation Pause) 2048M->512M(8192M) 15.234ms
[2024-04-15T10:35:20.456+0800][info][gc] GC(124) Pause Young (Normal) (G1 Evacuation Pause) 2560M->640M(8192M) 18.567ms
[2024-04-15T10:40:25.789+0800][info][gc] GC(125) Pause Full (Allocation Failure) 4096M->1024M(8192M) 156.789ms
```

**解读**：
- `GC(123)`：第 123 次 GC
- `Pause Young`：Young GC
- `2048M->512M`：GC 前堆使用 2048MB，GC 后 512MB
- `(8192M)`：堆总大小 8192MB
- `15.234ms`：GC 停顿时间

---

### 5.3 关键指标

| 指标 | 正常范围 | 异常处理 |
|------|---------|---------|
| **Young GC 频率** | 每秒 < 1 次 | 增大新生代 |
| **Young GC 停顿** | < 50ms | 检查对象分配速率 |
| **Full GC 频率** | 每小时 < 1 次 | 检查内存泄漏 |
| **Full GC 停顿** | < 500ms | 增大堆或调整 IHOP |
| **堆使用率** | 60-80% | 调整堆大小 |

---

## 六、内存泄漏排查

### 6.1 症状识别

**可能的内存泄漏迹象**：
- ✅ 堆使用率持续增长，GC 后无法回收
- ✅ Full GC 频率越来越高
- ✅ 应用运行几天后 OOM
- ✅ GC 日志显示 Old Gen 持续增长

---

### 6.2 排查步骤

#### 步骤1：生成堆转储文件

```bash
# 方式1：OOM 时自动生成（需配置 -XX:+HeapDumpOnOutOfMemoryError）

# 方式2：手动触发
jmap -dump:format=b,file=/tmp/heapdump.hprof <pid>

# 方式3：使用 jcmd
jcmd <pid> GC.heap_dump /tmp/heapdump.hprof
```

---

#### 步骤2：分析堆转储

**工具1：Eclipse MAT**

```bash
# 下载 MAT
wget https://www.eclipse.org/mat/downloads.php

# 打开 heapdump.hprof
# 1. 打开 Histogram 视图
# 2. 按 Retained Heap 排序
# 3. 查找占用内存最多的对象
# 4. 右键 → Path to GC Roots → exclude weak references
```

**工具2：JProfiler**

```bash
# 商业工具，功能更强大
# 1. 连接正在运行的 JVM
# 2. 查看 Memory 视图
# 3. 分析对象引用链
```

---

#### 步骤3：定位泄漏代码

**常见泄漏原因**：

**1. 静态集合未清理**
```java
// ❌ 错误示例
public class OrderCache {
    private static final Map<Long, Order> cache = new HashMap<>();
    
    public void addOrder(Order order) {
        cache.put(order.getId(), order);  // 只增不减
    }
}

// ✅ 正确示例：使用弱引用或定期清理
private static final Map<Long, WeakReference<Order>> cache = new ConcurrentHashMap<>();
```

**2. 未关闭的资源**
```java
// ❌ 错误示例
InputStream is = new FileInputStream(file);
// 忘记关闭

// ✅ 正确示例
try (InputStream is = new FileInputStream(file)) {
    // 使用资源
}
```

**3. ThreadLocal 未清理**
```java
// ❌ 错误示例
private static final ThreadLocal<UserContext> context = new ThreadLocal<>();

public void setUser(User user) {
    context.set(new UserContext(user));
}
// 忘记调用 context.remove()

// ✅ 正确示例
public void setUser(User user) {
    try {
        context.set(new UserContext(user));
        // 业务逻辑
    } finally {
        context.remove();  // 确保清理
    }
}
```

---

## 七、CPU 飙高问题定位

### 7.1 快速诊断

```bash
# 1. 找到 Java 进程
top -c

# 2. 查看进程内线程 CPU 使用率
top -H -p <pid>

# 3. 记录 CPU 最高的线程 ID（十进制）
# 例如：12345

# 4. 转换为十六进制
printf "%x\n" 12345
# 输出：3039

# 5. 查看线程堆栈
jstack <pid> | grep -A 20 "0x3039"
```

---

### 7.2 使用 Arthas 诊断

```bash
# 1. 下载 Arthas
curl -O https://arthas.aliyun.com/arthas-boot.jar

# 2. 启动 Arthas
java -jar arthas-boot.jar

# 3. 选择进程

# 4. 查看最忙的线程
thread -n 3

# 5. 查看线程堆栈
thread <thread-id>

# 6. 实时监控方法调用
monitor -c 5 com.eos.order.service.OrderServiceImpl createOrder

# 7. 追踪方法执行
trace com.eos.order.service.OrderServiceImpl createOrder
```

---

### 7.3 常见原因

| 原因 | 特征 | 解决方案 |
|------|------|---------|
| **死循环** | 单个线程 CPU 100% | 检查 while/for 循环 |
| **频繁 GC** | 多个线程 CPU 高 | 优化内存使用 |
| **序列化/反序列化** | JSON 处理耗时 | 优化数据结构 |
| **正则表达式** | Pattern.compile 频繁调用 | 缓存 Pattern |
| **锁竞争** | 大量 BLOCKED 线程 | 优化锁粒度 |

---

## 八、性能监控工具

### 8.1 命令行工具

| 工具 | 用途 | 示例 |
|------|------|------|
| **jstat** | GC 统计 | `jstat -gcutil <pid> 1000` |
| **jmap** | 内存快照 | `jmap -histo <pid>` |
| **jstack** | 线程堆栈 | `jstack <pid>` |
| **jcmd** | 综合诊断 | `jcmd <pid> VM.flags` |

---

### 8.2 可视化工具

| 工具 | 类型 | 特点 |
|------|------|------|
| **JVisualVM** | GUI | JDK 自带，功能全面 |
| **JMC** | GUI | JDK Mission Control，低开销 |
| **Arthas** | CLI/Web | 阿里开源，在线诊断 |
| **Prometheus + Grafana** | Web | 生产环境监控 |

---

### 8.3 Prometheus 监控 JVM

**依赖**（已添加）：
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**关键指标**：
```promql
# JVM 内存使用
jvm_memory_used_bytes{area="heap"}
jvm_memory_used_bytes{area="nonheap"}

# GC 次数
rate(jvm_gc_pause_seconds_count[5m])

# GC 停顿时间
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])

# 线程数
jvm_threads_live_threads
jvm_threads_daemon_threads
```

---

## 📝 总结

### 调优 checklist

- [ ] 堆内存设置合理（-Xms = -Xmx）
- [ ] 选择合适的 GC（G1/ZGC）
- [ ] GC 停顿时间在可接受范围
- [ ] Full GC 频率 < 1 次/小时
- [ ] 启用 GC 日志并定期分析
- [ ] 配置 OOM 堆转储
- [ ] 监控 JVM 关键指标
- [ ] 定期进行压力测试

### 最佳实践

1. **不要过早优化**：先监控，再调优
2. **小步迭代**：每次只调整一个参数
3. **充分测试**：调优后必须压测验证
4. **文档记录**：记录每次调整的原因和效果
5. **持续监控**：生产环境实时监控 JVM 指标

---

## 🔗 参考资料

- [Oracle JVM 官方文档](https://docs.oracle.com/en/java/javase/21/)
- [G1 GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Arthas 用户文档](https://arthas.aliyun.com/)
- 《深入理解Java虚拟机》- 周志明

---

**最后更新**：2024-04-15  
**维护者**：EOS Team
