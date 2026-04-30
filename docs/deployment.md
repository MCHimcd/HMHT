# Deployment Guide

本文说明如何使用 GitHub Actions、SSH 和 systemd 部署 HMHT Spring Boot 后端。当前工作流只覆盖后端 jar；前端 `frontend/dist/` 可单独部署为静态站点或由同机 Nginx 托管。

## Architecture

| Part | Role |
|------|------|
| GitHub Actions | 构建 `bootJar`，上传 jar，重启服务 |
| SSH | 将构建产物传到服务器 |
| systemd | 以服务方式运行 Spring Boot jar |
| MySQL | 由服务器环境提供，连接参数通过环境变量或配置文件指定 |

## GitHub Secrets

在仓库 `Settings -> Secrets and variables -> Actions` 中添加：

| Secret | Purpose | Example |
|--------|---------|---------|
| `SERVER_HOST` | 服务器 IP 或域名 | `123.45.67.89` |
| `SERVER_PORT` | SSH 端口 | `22` |
| `SERVER_USER` | SSH 登录用户 | `deploy` |
| `SERVER_SSH_KEY` | SSH 私钥完整 PEM 内容 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `APP_DIR` | 服务器部署目录 | `/opt/hmht` |
| `SYSTEMD_SERVICE_NAME` | systemd 服务名 | `hmht` |
| `SERVER_KNOWN_HOST` | SSH 主机指纹，推荐填写 | `ssh-keyscan` 输出 |

获取 `SERVER_KNOWN_HOST`：

```bash
ssh-keyscan -p <SERVER_PORT> <SERVER_HOST>
```

将输出的完整一行写入 Secret。未设置时，工作流会自动获取主机指纹；这只适合内网或受信任环境。

## Server Initialization

以下命令在远程服务器执行。示例使用 `deploy`、`/opt/hmht` 和服务名 `hmht`；实际值需与 GitHub Secrets 保持一致。

### Create Deploy User

```bash
sudo adduser --disabled-password --gecos "" deploy
```

已有专用用户时可跳过。

### Harden SSH

```bash
sudo bash -c 'cat >> /etc/ssh/sshd_config << "EOF"

# --- HMHT deploy hardening ---
Match User deploy
    PasswordAuthentication no
    PubkeyAuthentication yes
EOF'

sudo systemctl reload sshd
```

### Install Java 25

```bash
sudo apt update
sudo apt install -y openjdk-25-jdk
java -version
```

预期看到 `openjdk version "25...`。

### Prepare App Directory

```bash
sudo mkdir -p /opt/hmht
sudo chown -R deploy:deploy /opt/hmht
sudo chmod 750 /opt/hmht
ls -ld /opt/hmht
```

### Configure SSH Key

在本地生成部署密钥：

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f hmht_deploy_key
```

文件用途：

| File | Usage |
|------|-------|
| `hmht_deploy_key` | 填入 GitHub Secret `SERVER_SSH_KEY` |
| `hmht_deploy_key.pub` | 写入服务器 `/home/deploy/.ssh/authorized_keys` |

在服务器写入公钥并设置权限：

```bash
sudo mkdir -p /home/deploy/.ssh
sudo bash -c 'echo "ssh-ed25519 AAAA...replace-with-public-key" >> /home/deploy/.ssh/authorized_keys'
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

### Create systemd Service

```bash
sudo tee /etc/systemd/system/hmht.service > /dev/null << 'EOF'
[Unit]
Description=HMHT Spring Boot Application
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/hmht
ExecStart=/usr/bin/java -jar /opt/hmht/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable hmht
```

首次部署前可不手动启动；Actions 会上传 jar 后重启服务。

### Allow Service Restart

GitHub Actions 需要重启和查看服务状态：

```bash
sudo tee /etc/sudoers.d/deploy-hmht > /dev/null << 'EOF'
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart hmht, /bin/systemctl status hmht
EOF
sudo chmod 440 /etc/sudoers.d/deploy-hmht
sudo visudo -c
```

### Verify SSH Login

在本地使用同一把私钥测试：

```bash
ssh -i hmht_deploy_key -p <SERVER_PORT> deploy@<SERVER_HOST> "echo 'SSH login OK'"
ssh -i hmht_deploy_key -p <SERVER_PORT> deploy@<SERVER_HOST> \
  "sudo systemctl status hmht --no-pager"
```

## Workflow Behavior

`.github/workflows/deploy.yml` 当前为手动触发：

| Item | Current Value |
|------|---------------|
| Trigger | `workflow_dispatch` |
| JDK | Temurin Java 25 |
| Build command | `./gradlew clean bootJar -x test --no-daemon` |
| Uploaded jar | `${APP_DIR}/app.jar` |
| Spring profile | systemd drop-in sets `SPRING_PROFILES_ACTIVE=prod` |

注意：部署工作流当前跳过测试。如果需要用测试阻断部署，应同步修改 workflow 和本文档。

## Runtime Configuration

生产敏感配置不要提交到仓库。推荐用 systemd 环境变量或外部配置文件提供：

```ini
[Service]
Environment="HMHT_DB_URL=jdbc:mysql://127.0.0.1:3306/hmht?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
Environment="HMHT_DB_USER=hmht"
Environment="HMHT_DB_PASSWORD=change-me"
```

也可以使用外部配置文件：

```bash
nano /opt/hmht/application-prod.properties
```

systemd `ExecStart` 示例：

```ini
ExecStart=/usr/bin/java -jar /opt/hmht/app.jar \
  --spring.config.location=file:/opt/hmht/application-prod.properties
```

## Operations

```bash
sudo systemctl status hmht
sudo journalctl -u hmht -f
sudo systemctl restart hmht
```

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

## Frontend Deployment

后端部署不自动处理前端。前端构建命令：

```bash
cd frontend
npm run build
```

部署方式：

| Mode | Notes |
|------|-------|
| Static hosting | 将 `frontend/dist/` 上传到 Nginx、对象存储或静态站点服务 |
| Same-server Nginx | Nginx 托管 `dist/`，并把 `/api` 反向代理到 `localhost:8080` |
