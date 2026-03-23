# HMHT 自动部署指南

本项目使用 **GitHub Actions + SSH + systemd** 将 Spring Boot 应用自动部署到云服务器。

---

## 一、所需 GitHub Secrets

在仓库 **Settings → Secrets and variables → Actions** 中添加以下 Secrets：

| Secret 名称 | 说明 | 示例值 |
|---|---|---|
| `SERVER_HOST` | 云服务器 IP 或域名 | `123.45.67.89` |
| `SERVER_PORT` | SSH 端口 | `22` |
| `SERVER_USER` | SSH 登录用户名 | `deploy` |
| `SERVER_SSH_KEY` | SSH 私钥（完整 PEM 内容） | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `APP_DIR` | 服务器上的部署目录 | `/opt/hmht` |
| `SYSTEMD_SERVICE_NAME` | systemd 服务单元名称 | `hmht` |
| `SERVER_KNOWN_HOST` | 服务器 SSH 主机指纹（推荐填写，防中间人攻击） | 见下方说明 |

> **如何获取 `SERVER_KNOWN_HOST`**：在本地或服务器上执行
> ```bash
> ssh-keyscan -p <PORT> <HOST>
> ```
> 将输出的完整一行（如 `123.45.67.89 ssh-ed25519 AAAA...`）填入该 Secret。  
> 若不填写，工作流将自动获取（仅适合内网或受信任环境，存在一定安全风险）。

---

## 二、服务器初始化步骤

### 1. 安装 Java 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

### 2. 创建部署用户（推荐，可跳过直接用已有用户）

```bash
sudo adduser deploy
```

### 3. 创建部署目录

```bash
sudo mkdir -p /opt/hmht
sudo chown -R deploy:deploy /opt/hmht
```

### 4. 配置 SSH 公钥登录

将 GitHub Actions 使用的私钥对应的**公钥**追加到服务器：

```bash
# 在服务器上执行（公钥格式示例：ssh-rsa AAAAB3NzaC1yc2EA... user@host）
mkdir -p /home/deploy/.ssh
echo "ssh-rsa AAAAB3NzaC1yc2EA...（替换为你的完整公钥内容）" >> /home/deploy/.ssh/authorized_keys
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
```

### 5. 创建 systemd 服务文件

```bash
sudo nano /etc/systemd/system/hmht.service
```

填入以下内容（可按需调整）：

```ini
[Unit]
Description=HMHT Spring Boot Application
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/hmht
ExecStart=/usr/bin/java -jar /opt/hmht/app.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动并设置开机自启：

```bash
sudo systemctl daemon-reload
sudo systemctl enable hmht
sudo systemctl start hmht
sudo systemctl status hmht
```

### 6. 配置 sudo 免密码（Actions 远程重启服务必需）

```bash
sudo visudo
```

添加以下一行（将 `deploy` 替换为你实际的 `SERVER_USER`，将 `hmht` 替换为实际服务名）：

```
deploy ALL=NOPASSWD: /bin/systemctl restart hmht, /bin/systemctl status hmht
```

---

## 三、可修改的配置项清单

所有常见配置都集中在以下两个地方：

### A. 工作流文件：`.github/workflows/deploy.yml`

| 配置项 | 位置（关键字） | 说明 |
|---|---|---|
| 触发分支 | `branches: - master` | 改为 `main`、`release` 等 |
| JDK 版本 | `java-version: '21'` | 按项目实际 Java 版本修改 |
| 构建命令 | `./gradlew clean bootJar -x test` | 去掉 `-x test` 可启用测试（构建时间会增加，测试失败会阻断部署） |
| 服务器 jar 文件名 | `${{ secrets.APP_DIR }}/app.jar` | 修改后需同步 systemd `ExecStart` |

### B. 服务器 systemd 文件：`/etc/systemd/system/hmht.service`

| 配置项 | 字段 | 说明 |
|---|---|---|
| 运行用户 | `User=deploy` | 改为实际部署用户 |
| 工作目录 | `WorkingDirectory=/opt/hmht` | 改为实际部署路径 |
| 启动命令 | `ExecStart=...` | 可加 JVM 参数、改 profile、改 jar 路径 |
| 自动重启 | `Restart=always` / `RestartSec=5` | 按需调整重启策略 |

### C. GitHub Secrets（Settings → Secrets and variables → Actions）

换服务器时只需更新对应 Secret，无需修改工作流文件本身。

---

## 四、常用运维命令

```bash
# 查看服务状态
sudo systemctl status hmht

# 实时查看日志
sudo journalctl -u hmht -f

# 手动重启
sudo systemctl restart hmht
```

---

## 五、生产环境配置文件（推荐）

敏感配置（数据库密码、端口等）不要放入仓库，建议在服务器上单独维护：

```bash
# 在服务器上创建
nano /opt/hmht/application-prod.yml
```

然后修改 systemd `ExecStart` 引用该文件：

```ini
ExecStart=/usr/bin/java -jar /opt/hmht/app.jar \
  --spring.config.location=file:/opt/hmht/application-prod.yml \
  --spring.profiles.active=prod
```
