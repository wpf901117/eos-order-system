# SkyWalking 分布式链路追踪实施指南

> **版本**：SkyWalking 9.7.0  
> **适用场景**：微服务架构、分布式系统  
> **难度**：中级

---

## 📚 目录

- [一、SkyWalking 简介](#一skywalking-简介)
- [二、架构设计](#二架构设计)
- [三、部署步骤](#三部署步骤)
- [四、Java Agent 配置](#四java-agent-配置)
- [五、代码集成](#五代码集成)
- [六、监控指标](#六监控指标)
- [七、告警配置](#七告警配置)
- [八、最佳实践](#八最佳实践)

---

## 一、SkyWalking 简介

### 1.1 什么是 SkyWalking？

SkyWalking 是一个开源的 APM（应用性能监控）系统，专为微服务、云原生和容器化架构设计。

**核心功能**：
- ✅ 分布式链路追踪
- ✅ 服务拓扑图
- ✅ 性能指标监控
- ✅ 告警通知
- ✅ 日志关联

### 1.2 为什么选择 SkyWalking？

| 特性 | SkyWalking | Zipkin | Jaeger |
|------|-----------|--------|--------|
| **语言支持** | Java/.NET/Node.js/Go/Python | 多语言 | 多语言 |
| **存储** | ES/MySQL/TiDB/H2 | ES/MySQL | ES/Cassandra |
| **UI 界面** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **告警** | ✅ 内置 | ❌ | ❌ |
| **日志关联** | ✅ | ❌ | ⚠️ |
| **社区活跃度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 二、架构设计

```
┌──────────────────────────────────────────────────────────┐
│                    应用服务层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Order    │  │ User     │  │ Product  │               │
│  │ Service  │  │ Service  │  │ Service  │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
│       │             │             │                      │
│       └─────────────┼─────────────┘                      │
│                     ↓                                    │
│          SkyWalking Java Agent (自动埋点)                 │
└─────────────────────┼────────────────────────────────────┘
                      ↓ gRPC
┌──────────────────────────────────────────────────────────┐
│              OAP Server (后端服务)                        │
│  ┌──────────────────────────────────────────────┐       │
│  │  接收 Trace/Metrics/Logs                     │       │
│  │  数据分析与聚合                               │       │
│  │  告警规则引擎                                 │       │
│  └──────────────────────────────────────────────┘       │
└─────────────────────┬────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────────────────┐
│                数据存储层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Elastic  │  │  MySQL   │  │   TiDB   │               │
│  │Search    │  │          │  │          │               │
│  └──────────┘  └──────────┘  └──────────┘               │
└──────────────────────────────────────────────────────────┘
                      ↑
┌──────────────────────────────────────────────────────────┐
│                  UI 界面                                  │
│  ┌──────────────────────────────────────────────┐       │
│  │  拓扑图 / 链路追踪 / 性能指标 / 告警          │       │
│  └──────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────┘
```

---

## 三、部署步骤

### 3.1 下载 SkyWalking

```bash
# 下载 SkyWalking 9.7.0
wget https://archive.apache.org/dist/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz

# 解压
tar -xzf apache-skywalking-apm-9.7.0.tar.gz
cd apache-skywalking-apm-9.7.0
```

### 3.2 配置 OAP Server

**文件**：`config/application.yml`

```yaml
cluster:
  selector: ${SW_CLUSTER:standalone}
  standalone:

core:
  default:
    # 数据保留时间（天）
    recordDataTTL: ${SW_CORE_RECORD_DATA_TTL:3}
    metricsDataTTL: ${SW_CORE_METRICS_DATA_TTL:7}

storage:
  selector: ${SW_STORAGE:elasticsearch}
  elasticsearch:
    namespace: ${SW_NAMESPACE:"skywalking"}
    clusterNodes: ${SW_STORAGE_ES_CLUSTER_NODES:localhost:9200}
    protocol: ${SW_STORAGE_ES_HTTP_PROTOCOL:"http"}
    user: ${SW_ES_USER:""}
    password: ${SW_ES_PASSWORD:""}

receiver-sharing-server:
  default:
    restHost: ${SW_RECEIVER_SHARING_REST_HOST:0.0.0.0}
    restPort: ${SW_RECEIVER_SHARING_REST_PORT:12800}
    gRPCHost: ${SW_RECEIVER_SHARING_GRPC_HOST:0.0.0.0}
    gRPCPort: ${SW_RECEIVER_SHARING_GRPC_PORT:11800}
```

### 3.3 启动 OAP Server

```bash
# 方式1：直接启动
bin/oapService.sh

# 方式2：后台启动
nohup bin/oapService.sh > logs/oap.log 2>&1 &

# 验证启动
curl http://localhost:12800/graphql
```

### 3.4 启动 UI

```bash
# 启动 Web UI
bin/webappService.sh

# 访问 UI
# http://localhost:8080
```

### 3.5 Docker Compose 部署（推荐）

**文件**：`docker-compose-skywalking.yml`

```yaml
version: '3.8'

services:
  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  oap:
    image: apache/skywalking-oap-server:9.7.0
    depends_on:
      - elasticsearch
    environment:
      SW_STORAGE: elasticsearch
      SW_STORAGE_ES_CLUSTER_NODES: elasticsearch:9200
    ports:
      - "11800:11800"  # gRPC
      - "12800:12800"  # HTTP
    volumes:
      - ./config:/skywalking/config

  ui:
    image: apache/skywalking-ui:9.7.0
    depends_on:
      - oap
    environment:
      SW_OAP_ADDRESS: http://oap:12800
    ports:
      - "8080:8080"

volumes:
  es-data:
```

**启动命令**：
```bash
docker-compose -f docker-compose-skywalking.yml up -d
```

---

## 四、Java Agent 配置

### 4.1 下载 Agent

Agent 已包含在 SkyWalking 发行版中：
```
apache-skywalking-apm-bin/agent/
├── skywalking-agent.jar
├── config/
│   └── agent.config
└── plugins/
```

### 4.2 配置 Agent

**文件**：`agent/config/agent.config`

```properties
# 服务名称
agent.service_name=${SW_AGENT_NAME:eos-order-service}

# OAP Server 地址
collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:127.0.0.1:11800}

# 日志级别
logging.level=${SW_LOGGING_LEVEL:INFO}

# 采样率（10000 = 100%）
agent.sample_n_per_3_secs=${SW_AGENT_SAMPLE:-1}

# 忽略某些路径
agent.ignore_path=${SW_AGENT_IGNORE_PATH:/health,/actuator/*}

# 插件配置
plugin.mysql.trace_sql_parameters=true
plugin.springmvc.collect_http_params=true
```

### 4.3 启动应用时加载 Agent

**方式1：命令行参数**
```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=eos-order-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar eos-service-order-1.0.0-SNAPSHOT.jar
```

**方式2：环境变量**
```bash
export SW_AGENT_NAME=eos-order-service
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800

java -javaagent:/path/to/skywalking-agent.jar \
     -jar eos-service-order-1.0.0-SNAPSHOT.jar
```

**方式3：修改启动脚本**

编辑 `scripts/start-production.sh`：
```bash
JVM_OPTS="
-javaagent:/opt/skywalking/agent/skywalking-agent.jar
-Dskywalking.agent.service_name=eos-order-service
-Dskywalking.collector.backend_service=prod-oap:11800

# ... 其他 JVM 参数
"
```

---

## 五、代码集成

### 5.1 自动埋点（无需代码修改）

SkyWalking Agent 会自动拦截以下组件：
- ✅ Spring MVC Controller
- ✅ Spring RestTemplate
- ✅ MyBatis Mapper
- ✅ Redis 操作
- ✅ MySQL JDBC
- ✅ RocketMQ/Kafka
- ✅ Feign Client

### 5.2 手动埋点（自定义追踪）

**添加依赖**：
```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-trace</artifactId>
    <version>9.7.0</version>
</dependency>
```

**示例1：@Trace 注解**
```java
import org.apache.skywalking.apm.toolkit.trace.Trace;

@Service
public class OrderService {
    
    @Trace(operationName = "createOrder")
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 业务逻辑
        // 自动记录为 Span
    }
}
```

**示例2：手动创建 Span**
```java
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

public void complexBusinessLogic() {
    // 创建本地 Span
    ActiveSpan localSpan = TraceContext.newLocalSpan("businessLogic");
    localSpan.start();
    
    try {
        // 业务逻辑
        ActiveSpan.tag("param", "value");
        
    } catch (Exception e) {
        ActiveSpan.error(e);
        throw e;
    } finally {
        localSpan.stop();
    }
}
```

**示例3：跨线程追踪**
```java
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;

// 包装 Runnable
executor.submit(RunnableWrapper.of(() -> {
    // 子线程中的操作会自动关联到父线程的 Trace
    log.info("异步任务执行");
}));

// 包装 Callable
CompletableFuture.supplyAsync(CallableWrapper.of(() -> {
    return fetchData();
}), executor);
```

### 5.3 日志关联

**添加依赖**：
```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-logback-1.x</artifactId>
    <version>9.7.0</version>
</dependency>
```

**配置 Logback**：`logback-spring.xml`
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%tid] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

**效果**：
```
2024-04-15 10:30:15.123 [http-nio-8083-exec-1] [TID:123456.789.1000] INFO  c.e.o.s.OrderServiceImpl - 订单创建成功
```

`[TID:123456.789.1000]` 就是 Trace ID，可以在 SkyWalking UI 中搜索。

---

## 六、监控指标

### 6.1 关键指标

**服务级别**：
- QPS（每秒请求数）
- 平均响应时间
- P50/P90/P95/P99 响应时间
- 错误率
- 成功率

**端点级别**：
- 每个 API 的性能指标
- 慢接口识别
- 异常接口识别

**数据库级别**：
- SQL 执行次数
- 平均执行时间
- 慢 SQL 检测
- 表访问频率

### 6.2 拓扑图

SkyWalking 自动生成服务拓扑图：
```
┌──────────┐      ┌──────────┐      ┌──────────┐
│  Gateway │─────▶│  Order   │─────▶│  MySQL   │
│          │      │  Service │      │          │
└──────────┘      └────┬─────┘      └──────────┘
                       │
                       ▼
                ┌──────────┐
                │  Redis   │
                └──────────┘
```

### 6.3 链路追踪详情

点击任意请求，可以看到完整的调用链：
```
Trace ID: 123456.789.1000

┌─ Gateway (5ms)
│  └─ OrderController.createOrder (150ms)
│     ├─ OrderService.createOrder (120ms)
│     │  ├─ ProductFeign.getProduct (30ms)
│     │  ├─ OrderMapper.insert (20ms)
│     │  └─ Redis.set (5ms)
│     └─ OrderCreatedEventHandler (25ms)
│        └─ NotificationService.send (20ms)
```

---

## 七、告警配置

### 7.1 告警规则

**文件**：`config/alarm-settings.yml`

```yaml
rules:
  # 服务响应时间告警
  - name: service_resp_time_rule
    metrics-name: service_resp_time
    op: ">"
    threshold: 1000  # 1秒
    period: 10  # 每10分钟检查
    count: 3  # 连续3次超过阈值
    silence-period: 5  # 静默期5分钟
    message: 服务 {name} 响应时间超过 1秒

  # 服务错误率告警
  - name: service_error_rate_rule
    metrics-name: service_error_rate
    op: ">"
    threshold: 0.05  # 5%
    period: 10
    count: 3
    silence-period: 5
    message: 服务 {name} 错误率超过 5%

  # 端点响应时间告警
  - name: endpoint_resp_time_rule
    metrics-name: endpoint_resp_time
    op: ">"
    threshold: 2000  # 2秒
    period: 10
    count: 3
    silence-period: 5
    message: 端点 {name} 响应时间超过 2秒

webhooks:
  # 钉钉 webhook
  - http://your-webhook-url/dingtalk
  # 企业微信 webhook
  - http://your-webhook-url/wechat
```

### 7.2 告警通知示例

**钉钉机器人**：
```json
{
  "msgtype": "markdown",
  "markdown": {
    "title": "SkyWalking 告警",
    "text": "### 告警信息\n- **服务**: eos-order-service\n- **指标**: 响应时间\n- **阈值**: 1000ms\n- **当前值**: 1523ms\n- **时间**: 2024-04-15 10:30:15"
  }
}
```

---

## 八、最佳实践

### 8.1 性能优化

**1. 采样率调整**
```properties
# 生产环境：降低采样率，减少开销
agent.sample_n_per_3_secs=100  # 约 1%

# 测试环境：全量采样
agent.sample_n_per_3_secs=10000  # 100%
```

**2. 忽略健康检查接口**
```properties
agent.ignore_path=/health,/actuator/*,/ping
```

**3. 异步发送数据**
```properties
# 默认就是异步，无需配置
```

### 8.2 问题排查

**问题1：Trace ID 不连续**

**原因**：采样率设置过低  
**解决**：提高 `agent.sample_n_per_3_secs`

**问题2：UI 看不到数据**

**排查步骤**：
1. 检查 Agent 是否正确加载
   ```bash
   java -jar app.jar 2>&1 | grep "SkyWalking"
   ```
2. 检查 OAP Server 是否正常运行
   ```bash
   curl http://localhost:12800/graphql
   ```
3. 检查网络连接
   ```bash
   telnet localhost 11800
   ```

**问题3：性能下降**

**优化方案**：
1. 降低采样率
2. 减少不必要的插件
3. 升级 SkyWalking 版本

### 8.3 生产环境建议

1. **高可用部署**
   - OAP Server 集群（至少2个节点）
   - Elasticsearch 集群（至少3个节点）
   - 负载均衡

2. **数据保留策略**
   ```yaml
   core:
     default:
       recordDataTTL: 3  # Trace 数据保留3天
       metricsDataTTL: 7  # 指标数据保留7天
   ```

3. **监控 SkyWalking 自身**
   - 监控 OAP Server 的 CPU、内存
   - 监控 Elasticsearch 集群状态
   - 设置告警

---

## 🚀 快速开始

### 5分钟快速体验

```bash
# 1. 启动 SkyWalking（Docker）
docker-compose -f docker-compose-skywalking.yml up -d

# 2. 下载 Agent
wget https://archive.apache.org/dist/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz
tar -xzf apache-skywalking-apm-9.7.0.tar.gz

# 3. 启动应用
java -javaagent:apache-skywalking-apm-bin/agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=test-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar your-app.jar

# 4. 访问 UI
# http://localhost:8080

# 5. 发起几个请求，查看拓扑图和链路追踪
```

---

## 🔗 参考资料

- [SkyWalking 官方文档](https://skywalking.apache.org/docs/)
- [SkyWalking GitHub](https://github.com/apache/skywalking)
- [SkyWalking 中文文档](https://skywalking.apache.org/zh/)

---

**最后更新**：2024-04-15  
**维护者**：EOS Team
