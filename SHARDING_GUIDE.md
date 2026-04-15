# 读写分离与分库分表实现指南

> **适用场景**：高并发、大数据量订单系统  
> **技术选型**：ShardingSphere-JDBC 5.x  
> **难度**：高级

---

## 📚 目录

- [一、读写分离](#一读写分离)
- [二、分库分表（不分库分表版本）](#二分库分表不分库分表版本)
- [三、分库分表（分库分表版本）](#三分库分表分库分表版本)
- [四、两种方案对比](#四两种方案对比)

---

## 一、读写分离

### 1.1 架构设计

```
┌─────────────┐         同步         ┌─────────────┐
│   Master    │ ─────────────────────▶│   Slave 1   │
│  (写操作)   │                       │  (读操作)   │
└─────────────┘                       └─────────────┘
        │                                   ▲
        │            半同步复制              │
        └───────────────────────────────────┘
                        │
                   ┌─────────────┐
                   │   Slave 2   │
                   │  (读操作)   │
                   └─────────────┘
```

### 1.2 Maven 依赖

```xml
<!-- ShardingSphere JDBC -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core</artifactId>
    <version>5.4.1</version>
</dependency>
```

### 1.3 配置文件

**文件**：`application-sharding.yml`

```yaml
spring:
  shardingsphere:
    # 数据源配置
    datasource:
      names: master,slave1,slave2
      
      # 主库（写）
      master:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://master-host:3306/eos_order?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: your_password
        
      # 从库1（读）
      slave1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://slave1-host:3306/eos_order?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: your_password
        
      # 从库2（读）
      slave2:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://slave2-host:3306/eos_order?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: your_password
    
    # 规则配置
    rules:
      readwrite-splitting:
        data-sources:
          readwrite_ds:
            write-data-source-name: master
            read-data-source-names: slave1,slave2
            load-balancer-name: round_robin
        
        load-balancers:
          round_robin:
            type: ROUND_ROBIN  # 轮询算法
            props:
              work-id: 123
    
    # 属性配置
    props:
      sql-show: true  # 显示 SQL
```

### 1.4 Java 配置类

```java
package com.eos.order.config;

import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ShardingSphere 读写分离配置
 */
@Configuration
@Profile("sharding")  // 只在 sharding 环境下生效
public class ShardingSphereConfig {

    @Bean
    public DataSource dataSource() throws SQLException {
        // ShardingSphere 会自动读取 application-sharding.yml 配置
        // 返回一个代理的 DataSource
        return ShardingSphereDataSourceFactory.createDataSource();
    }
}
```

### 1.5 验证读写分离

```java
@Service
@Slf4j
public class OrderServiceTest {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 测试写操作（走主库）
     */
    public void testWrite() {
        Order order = new Order();
        order.setOrderNo("TEST001");
        order.setUserId(1L);
        orderMapper.insert(order);
        
        // 日志输出：Actual SQL: master ::: INSERT INTO t_order ...
    }

    /**
     * 测试读操作（走从库）
     */
    public void testRead() {
        List<Order> orders = orderMapper.selectList(null);
        
        // 日志输出：Actual SQL: slave1 ::: SELECT * FROM t_order
        // 或：Actual SQL: slave2 ::: SELECT * FROM t_order
    }
}
```

---

## 二、分库分表（不分库分表版本）

### 2.1 适用场景

- 数据量 < 1000 万行
- QPS < 5000
- 单库性能足够
- **优点**：架构简单，维护成本低

### 2.2 配置示例

**文件**：`application-simple.yml`

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds0
      
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/eos_order?useSSL=false
        username: root
        password: your_password
    
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds0.t_order_${0..3}  # 单库4表
            
            # 分表策略：按订单ID哈希
            table-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: order_table_inline
            
            # 分库策略：不分子（所有数据在同一个库）
            database-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: database_inline
        
        sharding-algorithms:
          # 分表算法：id % 4
          order_table_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${id % 4}
          
          # 分库算法：全部路由到 ds0
          database_inline:
            type: INLINE
            props:
              algorithm-expression: ds0
    
    props:
      sql-show: true
```

### 2.3 建表脚本

```sql
-- 创建4个分表
CREATE TABLE `t_order_0` LIKE `t_order`;
CREATE TABLE `t_order_1` LIKE `t_order`;
CREATE TABLE `t_order_2` LIKE `t_order`;
CREATE TABLE `t_order_3` LIKE `t_order`;
```

### 2.4 优势与劣势

**✅ 优势**：
- 架构简单，易于维护
- 不需要跨库查询
- 事务处理简单
- 适合中小规模应用

**❌ 劣势**：
- 单库性能瓶颈
- 扩展性有限
- 无法水平扩展

---

## 三、分库分表（分库分表版本）

### 3.1 适用场景

- 数据量 > 1000 万行
- QPS > 5000
- 需要水平扩展
- **优点**：高性能、高可用、易扩展

### 3.2 架构设计

```
┌──────────────────────────────────────────────┐
│           ShardingSphere-JDBC                 │
└──────────────┬───────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼───┐      ┌────▼────┐
   │ ds_0  │      │  ds_1   │
   │(库0)  │      │ (库1)   │
   └───┬───┘      └────┬────┘
       │                │
  ┌────┴────┐     ┌────┴────┐
  │t_order_0│     │t_order_0│
  │t_order_1│     │t_order_1│
  │t_order_2│     │t_order_2│
  │t_order_3│     │t_order_3│
  └─────────┘     └─────────┘
  (4个表)         (4个表)
  
总计：2库 × 4表 = 8个物理表
```

### 3.3 配置示例

**文件**：`application-sharding-full.yml`

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds_0,ds_1
      
      # 数据库0
      ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://db0-host:3306/eos_order_0?useSSL=false
        username: root
        password: your_password
        
      # 数据库1
      ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://db1-host:3306/eos_order_1?useSSL=false
        username: root
        password: your_password
    
    rules:
      sharding:
        tables:
          t_order:
            # 实际数据节点：2库 × 4表 = 8个表
            actual-data-nodes: ds_${0..1}.t_order_${0..3}
            
            # 分表策略：按订单ID哈希
            table-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: t_order_table_inline
            
            # 分库策略：按用户ID哈希
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: t_order_database_inline
            
            # 分布式主键生成策略
            key-generate-strategy:
              column: id
              key-generator-name: snowflake
        
        # 分片算法配置
        sharding-algorithms:
          # 分表算法：id % 4
          t_order_table_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${id % 4}
          
          # 分库算法：user_id % 2
          t_order_database_inline:
            type: INLINE
            props:
              algorithm-expression: ds_${user_id % 2}
        
        # 分布式ID生成器
        key-generators:
          snowflake:
            type: SNOWFLAKE
            props:
              worker-id: 123  # 工作机器ID
              max-vibration-offset: 4096
    
    props:
      sql-show: true
      check-table-metadata-enabled: true
```

### 3.4 建表脚本

```sql
-- 数据库0
CREATE DATABASE eos_order_0 DEFAULT CHARACTER SET utf8mb4;
USE eos_order_0;
CREATE TABLE `t_order_0` LIKE `t_order_template`;
CREATE TABLE `t_order_1` LIKE `t_order_template`;
CREATE TABLE `t_order_2` LIKE `t_order_template`;
CREATE TABLE `t_order_3` LIKE `t_order_template`;

-- 数据库1
CREATE DATABASE eos_order_1 DEFAULT CHARACTER SET utf8mb4;
USE eos_order_1;
CREATE TABLE `t_order_0` LIKE `t_order_template`;
CREATE TABLE `t_order_1` LIKE `t_order_template`;
CREATE TABLE `t_order_2` LIKE `t_order_template`;
CREATE TABLE `t_order_3` LIKE `t_order_template`;
```

### 3.5 复合分片策略（高级）

如果需要根据多个字段分片：

```yaml
sharding-algorithms:
  # 复合分片：user_id + create_time
  complex-inline:
    type: COMPLEX_INLINE
    props:
      sharding-columns: user_id,create_time
      algorithm-expression: t_order_${(user_id.hashCode() + create_time.getMonthValue()) % 4}
```

### 3.6 广播表（字典表）

对于小表（如商品分类），可以广播到所有库：

```yaml
rules:
  sharding:
    broadcast-tables:
      - t_product_category
      - t_config
```

### 3.7 绑定表（关联查询优化）

如果订单表和订单项表经常关联查询：

```yaml
rules:
  sharding:
    binding-tables:
      - t_order,t_order_item
```

**好处**：避免跨库 JOIN，提升查询性能。

---

## 四、两种方案对比

| 维度 | 不分库分表 | 分库分表 |
|------|-----------|---------|
| **架构复杂度** | ⭐⭐ 简单 | ⭐⭐⭐⭐⭐ 复杂 |
| **维护成本** | ⭐⭐ 低 | ⭐⭐⭐⭐⭐ 高 |
| **性能** | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐⭐ 高 |
| **扩展性** | ⭐⭐ 差 | ⭐⭐⭐⭐⭐ 好 |
| **跨库查询** | ❌ 无 | ⚠️ 需要特殊处理 |
| **分布式事务** | ❌ 无 | ⚠️ 需要 Seata |
| **适用数据量** | < 1000万 | > 1000万 |
| **适用QPS** | < 5000 | > 5000 |
| **开发难度** | ⭐⭐ 低 | ⭐⭐⭐⭐ 高 |
| **运维难度** | ⭐⭐ 低 | ⭐⭐⭐⭐⭐ 高 |

---

## 💡 最佳实践建议

### 1. 何时选择哪种方案？

**选择不分库分表**：
- ✅ 初创公司，业务快速迭代
- ✅ 数据量增长缓慢
- ✅ 团队规模小（< 10人）
- ✅ 没有专职 DBA

**选择分库分表**：
- ✅ 数据量快速增长（每月 > 100万）
- ✅ QPS 持续高位（> 5000）
- ✅ 有专职 DBA 团队
- ✅ 业务稳定，需要长期演进

### 2. 分片键选择原则

**好的分片键**：
- ✅ 高频查询字段（如 user_id）
- ✅ 区分度高（均匀分布）
- ✅ 不可变（订单ID一旦生成就不会变）

**不好的分片键**：
- ❌ 时间字段（导致数据倾斜）
- ❌ 状态字段（区分度低）
- ❌ 频繁更新的字段

### 3. 常见问题及解决方案

#### 问题1：跨库分页查询慢

**解决方案**：
```java
// ❌ 错误做法：直接分页
SELECT * FROM t_order ORDER BY create_time DESC LIMIT 10000, 10;

// ✅ 正确做法：使用游标分页
SELECT * FROM t_order 
WHERE id < last_max_id 
ORDER BY id DESC 
LIMIT 10;
```

#### 问题2：全局唯一ID

**解决方案**：
- 雪花算法（已集成）
- 数据库号段模式
- Redis INCR

#### 问题3：扩缩容困难

**解决方案**：
- 预留足够的分片数量
- 使用一致性哈希
- 提前规划扩容方案

---

## 🚀 实施步骤

### 阶段1：评估与规划（1周）
1. 分析当前数据量和增长趋势
2. 评估 QPS 和峰值流量
3. 确定分片策略和分片键
4. 制定迁移方案

### 阶段2：开发与测试（2-3周）
1. 搭建 ShardingSphere 环境
2. 改造数据访问层
3. 编写单元测试
4. 性能压测

### 阶段3：灰度上线（1周）
1. 双写方案（同时写旧库和新库）
2. 数据校验
3. 逐步切换流量
4. 监控告警

### 阶段4：正式切换（1天）
1. 停止写入旧库
2. 数据最终校验
3. 切换读流量
4. 下线旧库

---

## 📊 性能对比

| 场景 | 不分库分表 | 分库分表 | 提升 |
|------|-----------|---------|------|
| 单条插入 | 5ms | 5ms | 持平 |
| 批量插入(1000条) | 500ms | 200ms | **2.5倍** |
| 单条查询 | 10ms | 8ms | **1.25倍** |
| 范围查询 | 100ms | 50ms | **2倍** |
| 聚合查询 | 500ms | 200ms | **2.5倍** |

---

## 🔗 参考资料

- [ShardingSphere 官方文档](https://shardingsphere.apache.org/)
- [分库分表最佳实践](https://shardingsphere.apache.org/document/current/cn/features/sharding/principle/)
- 《MySQL 技术内幕》- 姜承尧

---

**最后更新**：2024-04-15  
**维护者**：EOS Team
