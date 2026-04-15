#!/bin/bash

# ==================== GitHub 上传脚本 ====================
# 使用方法: ./upload-to-github.sh <repository-url>
# 例如: ./upload-to-github.sh https://github.com/your-username/eos-order-system.git

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  EOS 项目 - GitHub 上传脚本"
echo "=========================================="
echo ""

# 检查是否提供了仓库地址
if [ -z "$1" ]; then
    echo "❌ 错误: 请提供 GitHub 仓库地址"
    echo ""
    echo "使用方法:"
    echo "  ./upload-to-github.sh https://github.com/your-username/eos-order-system.git"
    echo ""
    echo "或者使用 SSH:"
    echo "  ./upload-to-github.sh git@github.com:your-username/eos-order-system.git"
    exit 1
fi

REPO_URL=$1

# 检查是否已初始化 Git
if [ ! -d ".git" ]; then
    echo "📦 步骤 1/5: 初始化 Git 仓库..."
    git init
    echo "✅ Git 仓库初始化成功"
else
    echo "✅ Git 仓库已存在"
fi
echo ""

# 添加 .gitignore
if [ -f ".gitignore" ]; then
    echo "✅ .gitignore 文件已存在"
else
    echo "⚠️  警告: 未找到 .gitignore 文件"
fi
echo ""

# 添加所有文件
echo "📦 步骤 2/5: 添加文件到暂存区..."
git add .
echo "✅ 文件添加成功"
echo ""

# 提交
echo "📦 步骤 3/5: 提交到本地仓库..."
git commit -m "Initial commit: EOS 企业级订单管理系统

项目特性:
- JDK 21 + Spring Boot 3.2 + Spring Cloud Alibaba
- 微服务架构（订单、商品、用户服务）
- DDD 领域驱动设计（聚合根、值对象、领域事件）
- RocketMQ 高级特性（事务消息、顺序消息）
- Redis 高级应用（分布式锁、缓存一致性策略）
- 多级缓存架构（Caffeine L1 + Redis L2 + 布隆过滤器）
- SkyWalking 链路追踪
- Sentinel 弹性设计（限流、熔断降级）
- 并发编程实战（线程池、CompletableFuture）
- 完整文档体系（16个技术文档）

技术栈:
- Spring Cloud Alibaba 2023.0.1.0
- MyBatis Plus 3.5.6
- Redisson 3.27.2
- RocketMQ 2.3.0
- Seata 分布式事务
- Prometheus + Grafana 监控
- Docker + Docker Compose"

echo "✅ 提交成功"
echo ""

# 关联远程仓库
echo "📦 步骤 4/5: 关联远程仓库..."
# 检查是否已存在 remote
if git remote | grep -q "origin"; then
    echo "⚠️  远程仓库 'origin' 已存在，正在更新..."
    git remote set-url origin "$REPO_URL"
else
    git remote add origin "$REPO_URL"
fi
echo "✅ 远程仓库关联成功: $REPO_URL"
echo ""

# 重命名分支
echo "📦 步骤 5/5: 准备推送..."
git branch -M main
echo "✅ 主分支已重命名为 'main'"
echo ""

# 显示摘要
echo "=========================================="
echo "  📊 上传摘要"
echo "=========================================="
echo ""
echo "远程仓库: $REPO_URL"
echo "分支名称: main"
echo ""
echo "提交统计:"
git log --oneline | head -1
echo ""
echo "文件统计:"
echo "  - Java 文件: $(find . -name "*.java" -not -path "./target/*" | wc -l | tr -d ' ')"
echo "  - 配置文件: $(find . -name "*.yml" -o -name "*.yaml" -o -name "*.properties" -not -path "./target/*" | wc -l | tr -d ' ')"
echo "  - 文档文件: $(find . -name "*.md" | wc -l | tr -d ' ')"
echo ""
echo "=========================================="
echo "  ⚠️  准备推送到 GitHub"
echo "=========================================="
echo ""
echo "即将执行: git push -u origin main"
echo ""
read -p "确认推送？(y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🚀 正在推送到 GitHub..."
    git push -u origin main
    
    echo ""
    echo "=========================================="
    echo "  ✅ 上传成功！"
    echo "=========================================="
    echo ""
    echo "访问你的仓库:"
    echo "$REPO_URL"
    echo ""
    echo "提示："
    echo "  1. 如果是第一次推送，可能需要输入 GitHub 用户名和密码"
    echo "  2. 推荐使用 SSH 方式避免每次输入密码"
    echo "  3. 可以在 GitHub 上启用 GitHub Actions 实现 CI/CD"
    echo ""
else
    echo ""
    echo "❌ 已取消推送"
    echo ""
    echo "如需手动推送，执行:"
    echo "  git push -u origin main"
    echo ""
fi
