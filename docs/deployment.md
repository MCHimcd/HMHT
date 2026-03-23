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

## 二、远程服务器完整初始化步骤（可直接复制执行）

> 以下所有命令均在**远程服务器**上执行。  
> 变量说明：`deploy` 对应 Secret `SERVER_USER`，`/opt/hmht` 对应 `APP_DIR`，`hmht` 对应 `SYSTEMD_SERVICE_NAME`。  
> 如果你的值不同，请全局替换后再执行。

---

### 步骤 1：创建专用部署用户

```bash
# 以 root 或具有 sudo 权限的用户执行
sudo adduser --disabled-password --gecos "" deploy
# 如果已有用户可跳过此步骤
```

---

### 步骤 2：SSH 加固（限制 deploy 用户登录方式）

```bash
# 禁止 deploy 用户通过密码登录（只允许公钥），编辑 sshd_config
sudo bash -c 'cat >> /etc/ssh/sshd_config << "EOF"

# --- HMHT deploy hardening ---
Match User deploy
    PasswordAuthentication no
    PubkeyAuthentication yes
EOF'

# 重载 SSH 服务使配置生效
sudo systemctl reload sshd
```

---

### 步骤 3：安装 Java 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
# 预期输出：openjdk version "21.x.x" ...
```

---

### 步骤 4：创建应用部署目录并设置权限

```bash
sudo mkdir -p /opt/hmht
sudo chown -R deploy:deploy /opt/hmht
sudo chmod 750 /opt/hmht
# 验证
ls -ld /opt/hmht
# 预期输出：drwxr-x--- ... deploy deploy ... /opt/hmht
```

---

### 步骤 5：配置 SSH 公钥登录

**先在本地（或 CI 机器）生成密钥对（若尚未有专用 deploy key）：**

```bash
# 在本地执行，生成 ed25519 密钥对
ssh-keygen -t ed25519 -C "github-actions-deploy" -f hmht_deploy_key
# 生成两个文件：
#   hmht_deploy_key      <- 私钥，填入 GitHub Secret: SERVER_SSH_KEY
#   hmht_deploy_key.pub  <- 公钥，粘贴到服务器 authorized_keys
```

**然后在服务器上将公钥写入 authorized_keys：**

```bash
# 以 root 或具有 sudo 权限的用户执行
sudo mkdir -p /home/deploy/.ssh

# 将上一步生成的 hmht_deploy_key.pub 内容替换下面引号中的内容
sudo bash -c 'echo "ssh-ed25519 AAAA...（替换为 hmht_deploy_key.pub 的完整内容）" >> /home/deploy/.ssh/authorized_keys'

# 设置正确的所有者和权限（SSH 对权限非常严格，必须精确）
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys

# 验证
sudo ls -la /home/deploy/.ssh
sudo cat /home/deploy/.ssh/authorized_keys
```

---

### 步骤 6：创建 systemd 服务文件

```bash
sudo tee /etc/systemd/system/hmht.service > /dev/null << 'EOF'
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
EOF
```

启用并启动服务（首次部署前可跳过 `start`，Actions 会自动重启）：

```bash
sudo systemctl daemon-reload
sudo systemctl enable hmht
sudo systemctl start hmht
sudo systemctl status hmht
```

---

### 步骤 7：配置 sudoers 免密码（Actions 远程重启服务必需）

```bash
# 安全写法：通过 visudo 或写入独立文件，避免语法错误锁死 sudo
sudo tee /etc/sudoers.d/deploy-hmht > /dev/null << 'EOF'
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart hmht, /bin/systemctl status hmht
EOF
sudo chmod 440 /etc/sudoers.d/deploy-hmht

# 验证语法正确（若有错误会报告，及时修正）
sudo visudo -c
```

---

### 步骤 8：验证 SSH 公钥登录（与 Actions 使用相同密钥对）

在**本地机器**上，用与 `SERVER_SSH_KEY` 相同的私钥测试连接：

```bash
# 将 hmht_deploy_key 路径替换为你的私钥文件
ssh -i hmht_deploy_key -p <SERVER_PORT> deploy@<SERVER_HOST> "echo 'SSH login OK'"
# 预期输出：SSH login OK

# 同时验证 sudo 免密码是否生效
ssh -i hmht_deploy_key -p <SERVER_PORT> deploy@<SERVER_HOST> \
  "sudo systemctl status hmht --no-pager"
```

如果以上命令均无报错，GitHub Actions 工作流即可正常运行。

---

### 步骤 9：获取 SERVER_KNOWN_HOST 并填入 Secrets

```bash
# 在本地或服务器上执行（替换端口和 IP）
ssh-keyscan -p <SERVER_PORT> <SERVER_HOST>
# 复制输出的完整一行（如：123.45.67.89 ssh-ed25519 AAAA...）
# 填入 GitHub Secret: SERVER_KNOWN_HOST
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

---

## 六、故障排查：Permission denied (publickey...)

当 GitHub Actions 日志出现以下错误时：

```
Permission denied, please try again.
Permission denied (publickey,gssapi-keyex,gssapi-with-mic,password).
Error: Process completed with exit code 255.
```

按以下顺序逐项排查：

### 原因 1：`SERVER_SSH_KEY` 填入的是公钥而非私钥

私钥格式（`SERVER_SSH_KEY` 必须是这种格式）：
```
-----BEGIN OPENSSH PRIVATE KEY-----
...（多行内容）...
-----END OPENSSH PRIVATE KEY-----
```
或：
```
-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----
```

公钥格式（**不能**填入 `SERVER_SSH_KEY`，这是错误的）：
```
ssh-ed25519 AAAA...
```

**检查方法**：进入 `Settings → Secrets and variables → Actions`，确认 `SERVER_SSH_KEY` 以 `-----BEGIN` 开头，而非 `ssh-`。

---

### 原因 2：服务器上没有写入对应的公钥

在服务器上验证：

```bash
# 查看 authorized_keys 内容
sudo cat /home/deploy/.ssh/authorized_keys

# 确认公钥指纹与私钥匹配（在本地执行）
ssh-keygen -lf hmht_deploy_key      # 私钥指纹
ssh-keygen -lf hmht_deploy_key.pub  # 公钥指纹（两者应相同）
```

---

### 原因 3：`~/.ssh` 目录或文件权限不正确

SSH 对权限极为严格，以下命令一次性修正：

```bash
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
# 验证
sudo ls -la /home/deploy/.ssh
```

权限必须精确匹配：`.ssh` 目录为 `700`，`authorized_keys` 文件为 `600`。

---

### 原因 4：`SERVER_HOST`、`SERVER_PORT`、`SERVER_USER` 填写错误

```bash
# 在本地用与 SERVER_SSH_KEY 相同的私钥测试连接
ssh -i hmht_deploy_key -p <SERVER_PORT> <SERVER_USER>@<SERVER_HOST> "echo OK"
# 若输出 OK 则说明认证完全正常
```

---

### 原因 5：服务器 sshd 配置禁用了公钥认证

```bash
# 在服务器上检查
sudo sshd -T | grep -E "pubkeyauthentication|authorizedkeysfile"
# 应输出：pubkeyauthentication yes
# 以及：authorizedkeysfile .ssh/authorized_keys ...
```

若 `pubkeyauthentication` 为 `no`，编辑 `/etc/ssh/sshd_config`，将其改为 `yes` 后重载：

```bash
sudo systemctl reload sshd
```

---

### 原因 6：`SERVER_KNOWN_HOST` 与实际服务器指纹不匹配

若服务器重装或 IP 更换过，需重新获取主机指纹：

```bash
ssh-keyscan -p <SERVER_PORT> <SERVER_HOST>
# 将输出结果更新到 GitHub Secret: SERVER_KNOWN_HOST
```

---

### 快速自检清单

| 检查项 | 验证命令 / 方法 |
|---|---|
| `SERVER_SSH_KEY` 是私钥 | 值以 `-----BEGIN` 开头 |
| 公钥已写入服务器 | `sudo cat /home/deploy/.ssh/authorized_keys` |
| `.ssh` 权限正确 | `sudo ls -la /home/deploy/.ssh` → `700` 和 `600` |
| 本地可用同私钥登录 | `ssh -i hmht_deploy_key -p PORT USER@HOST "echo OK"` |
| `SERVER_KNOWN_HOST` 已更新 | 重新运行 `ssh-keyscan` 并更新 Secret |
| sudoers 配置正确 | `ssh ... "sudo systemctl status hmht --no-pager"` |
