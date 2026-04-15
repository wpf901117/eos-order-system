#!/bin/bash

################################################################################
# EOS 订单服务 - 生产环境启动脚本
# 
# 使用方法：
#   ./start-production.sh [start|stop|restart|status]
#
# JVM 配置说明：
#   - 堆内存：8GB（可根据实际情况调整）
#   - GC：G1 收集器
#   - 目标停顿时间：< 200ms
#   - GC 日志：/data/logs/gc.log
#   - 堆转储：/data/logs/heapdump.hprof（OOM 时生成）
################################################################################

# 应用配置
APP_NAME="eos-order-service"
APP_JAR="target/eos-service-order-1.0.0-SNAPSHOT.jar"
APP_PORT=8083
PID_FILE="/tmp/${APP_NAME}.pid"
LOG_DIR="/data/logs"
STDOUT_LOG="${LOG_DIR}/app.log"
GC_LOG="${LOG_DIR}/gc.log"

# JVM 参数配置
JVM_OPTS="
# ==================== 堆内存配置 ====================
# 初始堆大小 = 最大堆大小，避免动态调整开销
-Xms8g
-Xmx8g

# ==================== G1 GC 配置 ====================
# 启用 G1 收集器（JDK 9+ 默认，显式声明更清晰）
-XX:+UseG1GC

# 最大 GC 停顿时间目标（毫秒）
# 根据业务容忍度调整：100-500ms
-XX:MaxGCPauseMillis=200

# 触发并发标记的堆占用百分比
# 降低可更早触发标记，减少 Full GC 风险
-XX:InitiatingHeapOccupancyPercent=45

# 并发标记线程数（默认为 CPU 核心数的 1/4）
# 增加可加快标记速度，但会占用更多 CPU
-XX:ConcGCThreads=4

# 并行 GC 线程数（默认为 CPU 核心数）
-XX:ParallelGCThreads=8

# 启用并行引用处理，提升 GC 效率
-XX:+ParallelRefProcEnabled

# ==================== 元空间配置 ====================
# 初始元空间大小
-XX:MetaspaceSize=256m

# 最大元空间大小（防止元空间泄漏导致 OOM）
-XX:MaxMetaspaceSize=512m

# ==================== GC 日志配置 ====================
# JDK 21 统一日志格式
# 输出到文件，保留 5 个文件，每个 100MB
-Xlog:gc*:file=${GC_LOG}:time,uptime:filecount=5,filesize=100M

# ==================== 堆转储配置 ====================
# OOM 时自动生成堆转储文件
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof

# ==================== 错误日志 ====================
# JVM 崩溃时生成错误日志
-XX:ErrorFile=${LOG_DIR}/hs_err_pid%p.log

# ==================== 容器支持 ====================
# 在容器中运行时，尊重 cgroup 限制
-XX:+UseContainerSupport

# ==================== 其他优化 ====================
# 使用 /dev/urandom 加速随机数生成
-Djava.security.egd=file:/dev/./urandom

# 文件编码
-Dfile.encoding=UTF-8

# 时区
-Duser.timezone=Asia/Shanghai
"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 JAR 文件是否存在
check_jar() {
    if [ ! -f "$APP_JAR" ]; then
        log_error "JAR 文件不存在: $APP_JAR"
        log_error "请先执行: mvn clean package -DskipTests"
        exit 1
    fi
}

# 创建日志目录
create_log_dir() {
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        log_info "创建日志目录: $LOG_DIR"
    fi
}

# 启动应用
start() {
    check_jar
    create_log_dir
    
    # 检查是否已经在运行
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            log_warn "$APP_NAME 已经在运行 (PID: $PID)"
            return 1
        else
            log_warn "删除过期的 PID 文件"
            rm -f "$PID_FILE"
        fi
    fi
    
    log_info "启动 $APP_NAME ..."
    log_info "JAR 文件: $APP_JAR"
    log_info "日志目录: $LOG_DIR"
    log_info "端口: $APP_PORT"
    
    # 启动应用
    nohup java $JVM_OPTS \
        -Dserver.port=$APP_PORT \
        -jar $APP_JAR \
        > "$STDOUT_LOG" 2>&1 &
    
    # 保存 PID
    echo $! > "$PID_FILE"
    
    log_info "$APP_NAME 已启动 (PID: $!)"
    log_info "查看日志: tail -f $STDOUT_LOG"
    log_info "查看 GC 日志: tail -f $GC_LOG"
}

# 停止应用
stop() {
    if [ ! -f "$PID_FILE" ]; then
        log_warn "$APP_NAME 未运行（找不到 PID 文件）"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    
    if ! ps -p $PID > /dev/null 2>&1; then
        log_warn "$APP_NAME 未运行（进程不存在）"
        rm -f "$PID_FILE"
        return 1
    fi
    
    log_info "停止 $APP_NAME (PID: $PID) ..."
    
    # 优雅关闭（发送 SIGTERM）
    kill $PID
    
    # 等待进程结束（最多 60 秒）
    for i in $(seq 1 60); do
        if ! ps -p $PID > /dev/null 2>&1; then
            log_info "$APP_NAME 已停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done
    
    # 强制杀死
    log_warn "优雅关闭超时，强制杀死进程"
    kill -9 $PID
    rm -f "$PID_FILE"
    log_info "$APP_NAME 已强制停止"
}

# 重启应用
restart() {
    stop
    sleep 2
    start
}

# 查看状态
status() {
    if [ ! -f "$PID_FILE" ]; then
        log_warn "$APP_NAME 未运行"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    
    if ps -p $PID > /dev/null 2>&1; then
        log_info "$APP_NAME 正在运行 (PID: $PID)"
        
        # 显示 JVM 信息
        echo ""
        echo "=== JVM 信息 ==="
        jcmd $PID VM.flags 2>/dev/null | head -20
        
        echo ""
        echo "=== GC 统计 ==="
        jstat -gcutil $PID 1000 3 2>/dev/null
        
        return 0
    else
        log_warn "$APP_NAME 未运行（进程不存在）"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 主逻辑
case "${1:-start}" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
