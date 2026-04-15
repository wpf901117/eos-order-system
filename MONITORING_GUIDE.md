# 监控与可观测性实现指南

> **优先级**：P0（最高优先级）  
> **状态**：✅ 已完成基础实现  
> **预计完成时间**：3天

---

## ✅ 已完成的工作

### 1. Actuator + Prometheus 监控集成

#### 1.1 添加的依赖

**父 POM** (`pom.xml`):
```xml
<!-- Actuator 监控端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus 注册表 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**订单服务** (`eos-service-order/pom.xml`):
- 已添加上述两个依赖

#### 1.2 配置文件

**application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

#### 1.3 自定义业务指标

**文件位置**: `eos-service/eos-service-order/src/main/java/com/eos/order/monitor/OrderMetrics.java`

**监控指标**:
- ✅ `order.create.total` - 订单创建总数（Counter）
- ✅ `order.pay.success.total` - 订单支付成功数（Counter）
- ✅ `order.cancel.total` - 订单取消数（Counter）
- ✅ `order.timeout.total` - 订单超时取消数（Counter）
- ✅ `order.create.duration` - 订单创建耗时（Timer）
- ✅ `order.pending.count` - 当前待支付订单数（Gauge）

**使用示例**:
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

---

## 📊 如何查看监控数据

### 1. 访问 Actuator 端点

启动订单服务后，访问以下地址：

```bash
# 健康检查
curl http://localhost:8083/actuator/health

# 应用信息
curl http://localhost:8083/actuator/info

# 所有指标
curl http://localhost:8083/actuator/metrics

# Prometheus 格式指标
curl http://localhost:8083/actuator/prometheus
```

### 2. Prometheus 采集配置

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'eos-order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8083']
        
  - job_name: 'eos-user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
      
  - job_name: 'eos-product-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']
```

### 3. Grafana 仪表盘

**导入 Dashboard JSON**（需要手动创建）:

关键查询语句：
```promql
# 订单创建速率（QPS）
rate(order_create_total[5m])

# 订单创建平均耗时
rate(order_create_duration_sum[5m]) / rate(order_create_duration_count[5m])

# 待支付订单数
order_pending_count

# 订单支付成功率
rate(order_pay_success_total[5m]) / rate(order_create_total[5m]) * 100
```

---

## 🔭 SkyWalking 分布式链路追踪（待实现）

### 为什么需要 SkyWalking？

- ✅ 全链路追踪：跨服务的调用链可视化
- ✅ 性能分析：慢接口自动识别
- ✅ 拓扑图：服务依赖关系自动发现
- ✅ 告警：异常自动通知

### 实施步骤

#### 步骤1：下载 SkyWalking

```bash
wget https://archive.apache.org/dist/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz
tar -xzf apache-skywalking-apm-9.7.0.tar.gz
cd apache-skywalking-apm-bin
```

#### 步骤2：启动 SkyWalking

```bash
# 启动 OAP Server
bin/oapService.sh

# 启动 UI
bin/webappService.sh
```

访问：http://localhost:8080

#### 步骤3：配置 Java Agent

**方式1：IDEA 配置**
```
VM Options:
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=eos-order-service
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

**方式2：启动脚本**
```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=eos-order-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar eos-service-order.jar
```

#### 步骤4：验证

1. 调用几个接口
2. 访问 SkyWalking UI：http://localhost:8080
3. 查看拓扑图和追踪链路

---

## 📈 ELK 日志聚合（待实现）

### 架构

```
应用日志 → Filebeat → Logstash → Elasticsearch → Kibana
```

### 实施步骤

#### 步骤1：启动 ELK Stack

**docker-compose-elk.yml**:
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

  kibana:
    image: kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

  logstash:
    image: logstash:8.11.0
    ports:
      - "5044:5044"
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch

  filebeat:
    image: elastic/filebeat:8.11.0
    volumes:
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml
      - /var/log/app:/var/log/app:ro
    depends_on:
      - logstash

volumes:
  es-data:
```

#### 步骤2：配置 Filebeat

**filebeat.yml**:
```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.log

output.logstash:
  hosts: ["logstash:5044"]
```

#### 步骤3：配置 Logstash

**logstash.conf**:
```
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "eos-logs-%{+YYYY.MM.dd}"
  }
}
```

#### 步骤4：访问 Kibana

访问：http://localhost:5601
- 创建索引模式：`eos-logs-*`
- 查看日志

---

## 🎯 下一步计划

### Phase 1: 完善监控（本周）

- [x] Actuator + Prometheus 集成
- [x] 自定义业务指标
- [ ] SkyWalking 链路追踪
- [ ] Grafana 仪表盘配置

### Phase 2: 日志聚合（下周）

- [ ] ELK Stack 部署
- [ ] Filebeat 配置
- [ ] Kibana 可视化

### Phase 3: 告警系统（下下周）

- [ ] AlertManager 配置
- [ ] 钉钉/企业微信告警
- [ ] 邮件告警

---

## 📝 学习要点

### 1. 监控四黄金信号

1. **延迟（Latency）**：请求处理时间
2. **流量（Traffic）**：QPS、并发数
3. **错误（Errors）**：错误率
4. **饱和度（Saturation）**：资源利用率

### 2. RED 方法

- **Rate**：请求速率
- **Errors**：错误数
- **Duration**：响应时间

### 3. USE 方法

- **Utilization**：资源利用率
- **Saturation**：饱和度
- **Errors**：错误数

---

## 🔗 相关文档

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/)
- [Prometheus](https://prometheus.io/)
- [SkyWalking](https://skywalking.apache.org/)
- [ELK Stack](https://www.elastic.co/elastic-stack)

---

**最后更新**：2024-04-15  
**维护者**：EOS Team
