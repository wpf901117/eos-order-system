package com.eos.common.util;

/**
 * 雪花算法（Snowflake）分布式ID生成器
 *
 * <p>在分布式系统中，传统的数据库自增ID存在以下问题：</p>
 * <ul>
 *   <li>单点瓶颈：所有写操作都依赖数据库的ID生成</li>
 *   <li>数据迁移困难：自增ID在分库分表后容易冲突</li>
 *   <li>信息安全：自增ID容易被猜测和遍历</li>
 * </ul>
 *
 * <p>雪花算法生成的64位Long型ID具有以下特点：</p>
 * <pre>
 * 0|0000000000 0000000000 0000000000 0000000000 0|00000|00000|0000000000 00
 * ^|                    41位时间戳                    | 机器ID | 序列号  |
 * 符号位              （毫秒级，可用69年）              （10位）  （12位）
 * </pre>
 *
 * <p><strong>结构解析：</strong></p>
 * <ul>
 *   <li>1位符号位：始终为0，保证ID为正数</li>
 *   <li>41位时间戳：当前时间减去起始时间戳，支持69年</li>
 *   <li>10位机器ID：支持1024个节点（通常拆分为5位数据中心+5位机器）</li>
 *   <li>12位序列号：每毫秒每个节点最多生成4096个ID</li>
 * </ul>
 *
 * <p><strong>性能指标：</strong></p>
 * <ul>
 *   <li>单机理论QPS：409.6万/秒</li>
 *   <li>ID趋势递增：对MySQL B+树索引友好</li>
 *   <li>不依赖外部存储：纯内存计算，零网络开销</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
public class SnowflakeUtil {

    /** 起始时间戳：2024-01-01 00:00:00 */
    private static final long START_TIMESTAMP = 1704067200000L;

    /** 机器ID所占位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心ID所占位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 序列号所占位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器ID最大值：31 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 数据中心ID最大值：31 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 序列号最大值：4095 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 时间戳左移位数：22位（5+5+12） */
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + DATACENTER_ID_BITS + SEQUENCE_BITS;

    /** 数据中心ID左移位数：17位（5+12） */
    private static final long DATACENTER_ID_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    /** 机器ID左移位数：12位 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心ID（生产环境从配置中心读取） */
    private final long datacenterId;

    /** 机器ID（生产环境从注册中心或环境变量读取） */
    private final long workerId;

    /** 毫秒内序列号 */
    private long sequence = 0L;

    /** 上次生成ID的时间戳 */
    private long lastTimestamp = -1L;

    /** 单例实例 */
    private static volatile SnowflakeUtil instance;

    /**
     * 获取单例实例
     *
     * @param datacenterId 数据中心ID（0-31）
     * @param workerId     机器ID（0-31）
     * @return SnowflakeUtil实例
     */
    public static SnowflakeUtil getInstance(long datacenterId, long workerId) {
        if (instance == null) {
            synchronized (SnowflakeUtil.class) {
                if (instance == null) {
                    instance = new SnowflakeUtil(datacenterId, workerId);
                }
            }
        }
        return instance;
    }

    /**
     * 默认实例（数据中心0，机器0）
     * 仅用于开发和测试，生产环境必须传入真实的机器标识
     */
    public static SnowflakeUtil getInstance() {
        return getInstance(0L, 0L);
    }

    private SnowflakeUtil(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("数据中心ID必须介于 %d 和 %d 之间", 0, MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("机器ID必须介于 %d 和 %d 之间", 0, MAX_WORKER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个分布式ID
     *
     * @return 全局唯一的64位正整数
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检测：如果当前时间小于上次生成时间，说明系统时钟被调慢了
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 5ms内的小幅回拨，等待到跟上一次相同的时间戳
                try {
                    wait(offset << 1);
                    timestamp = currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(
                                String.format("时钟回拨异常，拒绝生成ID。回拨时长：%d ms", offset));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待时钟恢复时线程被中断", e);
                }
            } else {
                throw new RuntimeException(
                        String.format("时钟回拨异常，拒绝生成ID。回拨时长：%d ms", offset));
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 按位运算组装64位ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待直到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前毫秒时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
