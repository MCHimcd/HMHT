---
description: "Use when modifying Docker Compose services, Dockerfile, GitHub Actions deployment workflow, environment variable configuration, or the start.ps1 local development launcher."
applyTo: "docker-compose.yml,Dockerfile,.github/workflows/**,start.ps1"
---

# Docker & 部署约定

## Docker Compose 服务编排

`docker-compose.yml` 定义 5 个服务的全链路部署：

| 服务 | 镜像/构建 | 端口 | 说明 |
|------|-----------|------|------|
| `influxdb` | `quay.io/influxdb/influxdb3-core` | 8181 | 时序数据库 |
| `agri-backend` | `agri_system/` 构建 | — | Python MQTT 后端 + SunAgent |
| `agri-web` | `agri_system/` 构建 | 5000 | Python Flask Web |
| `agri-mock-device` | `agri_system/` 构建 | — | Python 模拟设备 |
| `hmht` | `HMHT/` 构建 | 8080 | Java Spring Boot |

### 关键环境变量

| 变量 | 服务 | 值 | 说明 |
|------|------|-----|------|
| `MQTT_BROKER_HOST` | agri-* | `broker.emqx.io` | MQTT Broker 地址 |
| `HMHT_RUN_MODE` | hmht | `bridge` | 运行模式 |
| `HMHT_AGRI_CHAT_URL` | hmht | `http://agri-web:5000` | Python chat 服务地址 |
| `HMHT_INFLUXDB_URL` | hmht | `http://influxdb:8181` | InfluxDB 地址 |
| `HMHT_MOCK_SEED_ENABLED` | hmht | `true` | 种子数据开关 |
| `SPRING_DATASOURCE_URL` | hmht | `jdbc:mysql://host.docker.internal:3306/hmht?...` | MySQL（宿主机） |

### 修改注意事项

- MySQL 在宿主机运行（`host.docker.internal`），不在 Compose 中
- Python 服务依赖 MQTT Broker（公网 `broker.emqx.io`）
- `agri-backend` 需要 `AGENT_INTERVAL_MINUTES` 控制 SunAgent 决策周期

## 验证要求（脚本/部署类改动）

当改动仅限于以下范围时：`*.ps1`、`README.md`、`.github/instructions/**`、`docker-compose.yml`、`Dockerfile`、`.github/workflows/**`，且未涉及 Java 源码或 Gradle 构建文件：

- 无需执行 `./gradlew clean bootJar`
- 无需执行 `./gradlew test`
- 需执行与改动相关的验证（如 PowerShell 脚本冒烟、参数校验、文档引用一致性检查）

若改动包含 Java 源码、测试源码、Gradle 配置或会影响 Java 运行行为，则恢复执行完整 gradlew 构建与测试。
- 修改服务端口或环境变量后需同步检查其他服务的引用

## Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/HMHT-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**构建前**必须先执行 `./gradlew clean bootJar` 生成 JAR。

## GitHub Actions 部署工作流

**文件**：`.github/workflows/deploy.yml`

### 触发方式

- `workflow_dispatch`（手动触发）

### 流程

1. 检出代码
2. 安装 Java 21（Eclipse Temurin）
3. `./gradlew clean bootJar -x test`
4. SSH 上传 JAR 到远程服务器
5. 重启 systemd 服务

### 所需 GitHub Secrets

| Secret | 说明 |
|--------|------|
| `SERVER_HOST` | 远端服务器 IP/域名 |
| `SERVER_PORT` | SSH 端口 |
| `SERVER_USER` | SSH 用户名 |
| `SERVER_SSH_KEY` | SSH 私钥 |
| `APP_DIR` | 服务器上的应用目录 |
| `SYSTEMD_SERVICE_NAME` | systemd 服务名 |

### 修改注意事项

- JAR 文件名通过 `find` 自动查找（不硬编码）
- 修改 JDK 版本需同步更新 `build.gradle` 的 `java.toolchain.languageVersion`
- 详细部署指南参见 `docs/deployment.md`

## start.ps1（本地开发启动脚本）

### 使用方式

```powershell
# Mock 模式（仅 Java）
.\start.ps1 -Mode mock

# Bridge 模式（全链路）
.\start.ps1 -Mode bridge
```

### 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `-Mode` | `mock` | 运行模式：`mock` / `bridge` |
| `-PythonDir` | `d:\DevEcoStudioProjects\agri\agri_system\backend` | Python 源码目录 |
| `-JavaDir` | `d:\JetBrainsProjects\HMHT` | Java 项目目录 |
| `-AgriChatPort` | `5000` | Flask Web 端口 |
| `-InfluxDir` | — | InfluxDB 可执行文件目录 |
| `-InfluxToken` | — | InfluxDB 访问令牌 |

### 行为

- 强依赖 Windows Terminal（wt.exe）→ 单窗口多标签启动
- 主终端实时显示标签页状态
- bridge 模式启动：InfluxDB(可选) → mock_device.py → backend_main_debug.py → web_server_debug.py → gradlew bootRun
- mock 模式仅启动 Java
- Ctrl+C 统一停机（优雅优先，超时强制）并同步关闭标签页/子进程

### 修改注意事项

- 修改环境变量名称需同步更新此脚本中的 `set` 命令
- 修改默认路径需确认团队成员的目录结构
- 新增 Python 服务需在 bridge 模式启动列表中添加
