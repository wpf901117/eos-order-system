# GitHub 上传快速指南

## 🚀 快速开始（3步完成）

### 方法1：使用自动化脚本（推荐）

```bash
# 1. 给脚本添加执行权限
chmod +x upload-to-github.sh

# 2. 运行脚本（替换为你的仓库地址）
./upload-to-github.sh https://github.com/your-username/eos-order-system.git

# 或者使用 SSH（推荐）
./upload-to-github.sh git@github.com:your-username/eos-order-system.git
```

---

### 方法2：手动执行命令

```bash
# 1. 初始化 Git 仓库
cd /Users/wpf/claude_project/java-project
git init

# 2. 添加所有文件
git add .

# 3. 提交
git commit -m "Initial commit: EOS 企业级订单管理系统"

# 4. 关联远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/your-username/eos-order-system.git

# 5. 重命名分支并推送
git branch -M main
git push -u origin main
```

---

## 📋 详细步骤

### 第一步：创建 GitHub 仓库

1. 访问 https://github.com
2. 点击右上角 `+` → `New repository`
3. 填写信息：
   - **Repository name**: `eos-order-system`
   - **Description**: `企业级订单管理系统 - Spring Cloud Alibaba + JDK 21`
   - **Visibility**: Public 或 Private
   - **不要勾选** "Initialize this repository with a README"
4. 点击 `Create repository`
5. 复制仓库地址（HTTPS 或 SSH）

---

### 第二步：配置 Git（首次使用需要）

```bash
# 配置用户名和邮箱
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"

# 验证配置
git config --global user.name
git config --global user.email
```

---

### 第三步：设置 SSH Key（可选但推荐）

**为什么使用 SSH？**
- ✅ 无需每次输入密码
- ✅ 更安全
- ✅ 更方便

**生成 SSH Key：**

```bash
# 1. 生成密钥（如果已有可跳过）
ssh-keygen -t ed25519 -C "your-email@example.com"

# 2. 启动 ssh-agent
eval "$(ssh-agent -s)"

# 3. 添加密钥到 agent
ssh-add ~/.ssh/id_ed25519

# 4. 复制公钥
cat ~/.ssh/id_ed25519.pub

# 5. 添加到 GitHub
# - 访问 https://github.com/settings/keys
# - 点击 "New SSH key"
# - 粘贴公钥内容
# - 点击 "Add SSH key"

# 6. 测试连接
ssh -T git@github.com
```

---

### 第四步：上传代码

**使用脚本（推荐）：**
```bash
chmod +x upload-to-github.sh
./upload-to-github.sh git@github.com:your-username/eos-order-system.git
```

**或手动执行：**
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin git@github.com:your-username/eos-order-system.git
git branch -M main
git push -u origin main
```

---

## 🔧 常见问题

### 问题1：推送时需要输入密码

**解决方案1：使用个人访问令牌（PAT）**
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 选择权限：`repo`（全选）
4. 生成后复制令牌
5. 推送时使用令牌作为密码

**解决方案2：改用 SSH**
```bash
# 删除现有的 HTTPS remote
git remote remove origin

# 添加 SSH remote
git remote add origin git@github.com:your-username/eos-order-system.git

# 重新推送
git push -u origin main
```

---

### 问题2：推送失败 - rejected

**原因**：远程仓库有内容（README、LICENSE 等）

**解决方案：**
```bash
# 方案1：强制推送（会覆盖远程内容，谨慎使用）
git push -f origin main

# 方案2：先拉取再推送（推荐）
git pull origin main --allow-unrelated-histories
git push -u origin main
```

---

### 问题3：文件大小超过限制

**原因**：Git LFS 未配置或文件过大

**解决方案：**
```bash
# 检查大文件
git rev-list --objects --all | grep -E "\.(jar|war|zip)$"

# 从 Git 中移除大文件
git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch target/*.jar' \
  --prune-empty --tag-name-filter cat -- --all

# 重新推送
git push -f origin main
```

**预防**：确保 `.gitignore` 已包含 `target/` 目录

---

### 问题4：中文乱码

**解决方案：**
```bash
# 设置 Git 编码
git config --global core.quotepath false
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8

# 重新提交
git add .
git commit --amend
```

---

## 📊 上传后验证

### 1. 检查 GitHub 仓库

访问你的仓库页面，确认：
- ✅ 所有文件都已上传
- ✅ 目录结构正确
- ✅ README.md 正常显示
- ✅ 文档链接可点击

### 2. 克隆验证

```bash
# 在其他目录克隆仓库
cd /tmp
git clone https://github.com/your-username/eos-order-system.git

# 检查文件
cd eos-order-system
ls -la
```

### 3. 编译测试

```bash
# 克隆后尝试编译
mvn clean compile

# 应该成功编译
```

---

## 🎯 后续优化建议

### 1. 添加 GitHub Actions CI/CD

创建 `.github/workflows/maven.yml`：

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn -B package --file pom.xml
```

### 2. 添加 License

```bash
# 在 GitHub 仓库页面
# 点击 "Add file" → "Create new file"
# 文件名: LICENSE
# 选择 MIT License 或其他
```

### 3. 添加 Topics（标签）

在 GitHub 仓库页面：
- 点击齿轮图标 ⚙️
- 添加 topics：
  - `java`
  - `spring-boot`
  - `microservices`
  - `ddd`
  - `rocketmq`
  - `redis`

### 4. 启用 GitHub Pages（文档网站）

如果使用 MkDocs 或 Jekyll：
- Settings → Pages
- 选择分支和目录
- 访问 `https://your-username.github.io/eos-order-system`

---

## 📝 项目统计

上传成功后，你可以在 GitHub 看到：

```
📦 项目统计
├─ Java 文件: ~100+ 个
├─ 配置文件: ~20+ 个
├─ 文档文件: 16 个
├─ 总代码行数: ~10,000+ 行
└─ 技术模块: 15+ 个
```

---

## 💡 提示

1. **定期推送**：每完成一个功能就推送一次
2. **清晰的提交信息**：说明做了什么改动
3. **使用分支**：新功能在独立分支开发
4. **Pull Request**：团队协作时使用 PR 审查代码
5. **Release**：重要版本打 Tag 发布

---

## 🔗 相关资源

- [GitHub 官方文档](https://docs.github.com/)
- [Git 官方文档](https://git-scm.com/doc)
- [SSH Key 配置指南](https://docs.github.com/en/authentication/connecting-to-github-with-ssh)
- [GitHub Actions 文档](https://docs.github.com/en/actions)

---

**祝你上传顺利！** 🎉

如有问题，欢迎查看上面的常见问题部分。
