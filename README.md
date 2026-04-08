# HMHT — 鸿蒙慧田智慧系统（后端）

> Spring Boot 后端服务，为鸿蒙慧田智慧农业 App（OpenHarmony）提供 REST API、MQTT 桥接和数据持久化。

**目录**：[技术栈](#技术栈) · [项目结构](#项目结构) · [运行模式](#运行模式) · [构建与运行](#构建与运行) · [REST API](#rest-api) · [数据模型](#数据模型清单) · [服务清单](#服务清单) · [MQTT 协议](#mqtt-协议) · [设计约定](#设计约定)

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 4.0.4 |
| 构建 | Gradle (Wrapper) | — |
| 关系数据库 | MySQL | 8.x |
| 数据库迁移 | Flyway | — |
| ORM | Spring Data JPA (Hibernate) | — |
| 安全 | Spring Security + OAuth2 Resource Server | — |
| MQTT | Eclipse Paho Client | 1.2.5 |
| 时序数据库 | InfluxDB 3 (influxdb3-java) | 1.0.0 |
| CI/CD | GitHub Actions | — |

## 项目结构

```
HMHT/
├── src/main/java/ww/wangnong/
│   ├── HmhtApplication.java          # 启动类
│   ├── web/                           # REST 控制器
│   ├── service/                       # 业务逻辑接口
│   ├── entity/                        # JPA 实体
│   ├── dto/                           # 数据传输对象
│   ├── repository/                    # Spring Data JPA 仓库
│   ├── mqtt/                          # MQTT 客户端（Paho）
│   ├── bridge/                        # MQTT/Python 桥接层
│   ├── influx/                        # InfluxDB 写入
│   ├── mock/                          # Mock 模式实现 & 种子数据
│   ├── config/                        # Spring 配置类
│   │   └── condition/                 # 运行模式条件注解
│   └── exception/                     # 全局异常处理
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/                  # Flyway 迁移
├── src/test/java/ww/wangnong/         # 测试（44 个测试类）
├── scripts/
│   ├── start.ps1                      # 一键启动脚本
│   └── tests/                         # 脚本级测试
├── docs/
│   ├── api-endpoints.md               # REST API 端点详情
│   ├── backend-hardware-protocol.md   # 后端↔硬件通讯协议（V2）
│   ├── hardware-team-action-guide.md  # 硬件成员执行指南
│   └── deployment.md                  # 云部署指南
└── .github/
    ├── copilot-instructions.md        # AI 编码指令（入口）
    └── instructions/                  # 分领域编码规范
```

## 运行模式

通过 `hmht.run.mode`（环境变量 `HMHT_RUN_MODE`）切换：

| 模式 | 说明 | 数据来源 |
|------|------|----------|
| **mock**（默认） | 仅 Java 服务，自动播种演示数据 | MockDataLifecycle 种子 + InfluxDB 30 天历史 |
| **bridge** | App ↔ Java ↔ Python | Python agri → MQTT → MqttBridge → App |
| **hardware** | App ↔ Java ↔ 硬件端 | 硬件 MQTT → MqttBridge → App |
| **full** | App ↔ Java ↔ Python ↔ 硬件 | 双源 MQTT → MqttBridge → App |

组件通过 `config/condition/` 包中的 5 个自定义条件注解（`@ConditionalOnMockMode` / `@ConditionalOnBridgeMode` / `@ConditionalOnPythonRoute` / `@ConditionalOnHardwareRoute` / `@ConditionalOnNoPythonRoute`）按模式激活，详见 `.github/instructions/java.instructions.md`。

> **V2 阶段能力**：硬件 telemetry/command/ack 全链路打通；设备能力与影子查询；命令 ACK 超时重试闭环（5s 超时、3 次重试）；控制面持久化。协议规范见 [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md)。

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HMHT_RUN_MODE` | `mock` | 运行模式 |
| `HMHT_MOCK_SEED_ENABLED` | `true` | mock 模式是否播种演示数据 |
| `HMHT_AGRI_CHAT_URL` | `http://localhost:5000` | Python agri chat 服务地址 |
| `HMHT_INFLUXDB_ENABLED` | `true` | 是否启用 InfluxDB |
| `HMHT_INFLUXDB_TOKEN` | （空） | InfluxDB 访问令牌 |
| `HMHT_COMMAND_RETRY_CHECK_INTERVAL_MS` | `5000` | ACK 超时扫描周期（毫秒） |
| `HMHT_COMMAND_RETRY_MAX_RETRIES` | `3` | 命令最大重试次数 |
| `HMHT_COMMAND_RETRY_DEFAULT_TIMEOUT_MS` | `5000` | 默认命令超时 |

默认关闭 Hibernate SQL 打印（`spring.jpa.show-sql=false`）；排查时可临时调高日志级别。

## 构建与运行

### 本地开发（mock 模式）

```bash
./gradlew bootRun
```

### 本地开发（全链路）

```powershell
.\scripts\start.ps1 -Mode full
```

支持参数：`-Mode mock|bridge|hardware|full`、`-PythonDir`、`-JavaDir`、`-AgriChatPort`、`-InfluxDir`、`-InfluxToken`。要求已安装 Windows Terminal（`wt.exe`）。

启动后在主终端实时展示各服务状态，Ctrl+C 统一停机。当 InfluxDB 已存在 `_admin` token 且脚本未持有明文时，会提示通过 `-InfluxToken` 显式传入。

### 启动脚本测试

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\tests\start.ps1.test.ps1
```

### 编译 & 测试

```bash
./gradlew clean bootJar   # 编译 → build/libs/HMHT-0.0.1-SNAPSHOT.jar
./gradlew test             # 测试 → build/reports/tests/test/index.html
```

#### 测试概况

| 层次 | 注解 | 数量 |
|------|------|------|
| DTO / 异常 | `@Test` (纯 JUnit) | 3 |
| 桥接与连接 | `@ExtendWith(MockitoExtension)` | 6 |
| Repository | `@DataJpaTest` (H2) | 11 |
| Service | `@ExtendWith(MockitoExtension)` | 10 |
| Controller | `@WebMvcTest` | 12 |
| 启动与基础 | `@SpringBootTest` | 2 |

共 **44 个测试类**。命名规则：`{method}_{scenario}_{expected}`；工厂：`TestDataFactory`；Spring Boot 4.x 使用 `@MockitoBean`。详见 `.github/instructions/java.instructions.md` 测试规范章节。

## REST API

基路径：`/api`（`server.servlet.context-path=/api`）

| Controller | 路径前缀 | 职责 | 端点数 |
|------------|----------|------|--------|
| `AuthController` | `/v1/auth` | 登录、注册、验证码、密码、资料、头像 | 10 |
| `NodeController` | `/v1/nodes` | 节点列表、详情、元数据、阈值 | 5 |
| `DeviceController` | `/v1/devices` | 设备能力快照、设备影子 | 2 |
| `SensorController` | `/v1/sensors`, `/v1/equipment` | 传感器历史、设备统计 | 2 |
| `AlertController` | `/v1/alerts` | 告警查询与处理 | 2 |
| `ScheduledTaskController` | `/v1/tasks` | 定时任务 CRUD | 5 |
| `ChatController` | `/v1/agri-qa` | AI 农业问答 | 1 |
| `ExtraController` | `/v1/traceability`, `/v1/cameras` | 溯源扫码、摄像头 | 4 |
| `AdminController` | `/v1/admin` | 节点管理、用户管理、权限 | 7 |
| `OperationLogController` | `/v1/admin/logs` | 操作日志 | 1 |
| `HealthController` | `/v1` | 前端可用性检查 | 1 |
| `InfraController` | `/infra` | 健康检查、MQTT 手动发布 | 2 |

逐接口详情见 [docs/api-endpoints.md](docs/api-endpoints.md)。

## 数据模型清单

所有实体位于 `ww.wangnong.entity`，使用 Lombok `@Getter @Setter`。

### 基础业务表

| 实体 | 表名 | 说明 |
|------|------|------|
| `User` | `users` | 用户（phone unique） |
| `Node` | `nodes` | 农业节点 |
| `SensorType` | `sensor_type` | 传感器类型定义 |
| `NodeSensor` | `node_sensor` | 节点传感器绑定 + 阈值 |
| `EquipmentType` | `equipment_type` | 设备类型定义 |
| `NodeEquipment` | `node_equipment` | 节点设备绑定 |
| `Alert` | `alerts` | 告警记录 |
| `CommandLog` | `command_logs` | 命令日志 |
| `ChatMessage` | `chat_messages` | 聊天消息 |
| `ScheduledTask` | `scheduled_tasks` | 定时任务 |
| `OperationLog` | `operation_logs` | 操作审计日志 |
| `VerificationCode` | `verification_codes` | 验证码 |
| `UserNodeAccess` | `user_node_access` | 用户-节点访问权限 |

### V2 控制面表

| 实体 | 表名 | 说明 |
|------|------|------|
| `CommandRequest` | `command_request` | 命令闭环主表 |
| `CommandAck` | `command_ack` | 命令回执流水 |
| `CommandRetryLog` | `command_retry_log` | 超时重试轨迹 |
| `DeviceProfile` | `device_profile` | 设备档案 |
| `DeviceCapability` | `device_capability` | 设备能力快照 |
| `CapabilitySensor` | `capability_sensor` | 传感器能力明细 |
| `CapabilityEquipment` | `capability_equipment` | 设备能力明细 |
| `DeviceShadow` | `device_shadow` | 设备状态影子 |
| `IngestAudit` | `ingest_audit` | 消息接入审计 |
| `IngestDeadletter` | `ingest_deadletter` | 消息死信队列 |

## 服务清单

Service 接口（`ww.wangnong.service`）根据运行模式由不同实现提供：

| 接口 | Mock 实现 | Bridge 实现 | 说明 |
|------|-----------|-------------|------|
| `AuthService` | `MockAuthService` | — | 认证 & 用户管理 |
| `NodeService` | `MockNodeService` | — | 节点元数据聚合 |
| `AlertService` | `MockAlertService` | — | 告警查询 & 处理 |
| `SensorService` | `MockSensorService` | `InfluxSensorService` | 传感器历史 |
| `DeviceService` | `DatabaseDeviceService` | — | 设备能力 & 影子 |
| `ChatService` | `MockChatService` | `AgriChatService` | AI 对话 |
| `ExtraService` | `MockExtraService` | — | 溯源 & 摄像头 |
| `AdminService` | `MockAdminService` | — | 管理员操作 |
| `ScheduledTaskService` | `MockScheduledTaskService` | — | 定时任务 |
| `OperationLogService` | — | — | 操作日志（直接实现） |

**补充组件**：`MockDataLifecycle`（种子数据）、`MockMqttSimulator` / `MockAlertGenerator` / `MockTaskExecutor`（mock 模拟）、`MqttBridge`（桥接）、`TelemetryTranslator`（遥测翻译）、`InfluxTelemetryWriter`（InfluxDB 写入）、`ProtocolPersistenceService`（控制面持久化）、`CommandAckTracker`（ACK 重试）、`PahoMqttService`（MQTT 客户端）、`MqttSubscriptionRegistry`（订阅注册）。

## MQTT 协议

协议详情见 [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md)，主题与桥接实现详见 `.github/instructions/mqtt.instructions.md`。

### 硬件直连主题（V2）

| 方向 | 主题 | 说明 |
|------|------|------|
| Device → HMHT | `/hmht/device/{deviceId}/telemetry` | 遥测上报 |
| HMHT → Device | `/hmht/device/{deviceId}/command` | 控制命令 |
| Device → HMHT | `/hmht/device/{deviceId}/ack` | 命令回执 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/ack` | 回执透传 |

### 历史兼容主题（agri）

| 方向 | 主题 | 说明 |
|------|------|------|
| Python → HMHT | `/agri/device/{deviceId}/telemetry` | Python 遥测 |
| Python → HMHT | `/agri/ai_vision/alert` | AI 视觉告警 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/telemetry` | 翻译后遥测 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/alert` | 翻译后告警 |
| App → Python | `…/command` → `/agri/device/{deviceId}/command` | 命令转发 |

### 桥接逻辑概要

`MqttBridge` 通过 `MqttSubscriptionRegistry` 绑定 7 个主题，实现：agri/硬件遥测翻译转发、App 命令双路转发、硬件 ACK 透传、AI 告警入库、capability 持久化、InfluxDB 写入、超时命令重试。保持历史 agri 主题兼容，App 侧无需修改订阅。

## 与 agri 项目的协作

在 **bridge 模式**下：

| Python 服务 | 文件 | HMHT 交互 |
|-------------|------|-----------|
| 后端主进程 | `backend_main_debug.py` | MQTT 桥接 |
| Web 服务 | `web_server_debug.py` | HTTP 代理（AgriChatService） |
| 模拟设备 | `mock_device.py` | MQTT 桥接 |

## 硬件团队对接

- 协议规范：[docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md)
- 执行清单：[docs/hardware-team-action-guide.md](docs/hardware-team-action-guide.md)

## 数据库迁移

- 迁移目录：`src/main/resources/db/migration/`
- 当前版本：V2（协议控制面 10 张表）
- 命名规范：`V{N}__{description}.sql`（双下划线）
- DDL 策略：Flyway 创建基础表，Hibernate `update` 补充变更
- 编写约定见 `.github/instructions/database.instructions.md`

## 安全架构

- **SecurityConfig**：当前所有请求放行（`permitAll()`）
- CORS 全开放，CSRF 已禁用
- Token：Controller 手动从 `Authorization: Bearer {token}` 提取，Service 层做业务校验

## 部署

详见 [docs/deployment.md](docs/deployment.md)。CI/CD：GitHub Actions → SSH → systemd；构建：`./gradlew clean bootJar -x test`；运行时：Eclipse Temurin JRE 21。

## 设计约定

### 统一响应格式

```json
{ "code": 200, "message": "success", "data": ... }
```

错误由 `BizException(code, message)` 抛出，`GlobalExceptionHandler` 捕获转为 `ApiResponse.error()`。

### 配置管理

- `@ConfigurationProperties` 用于结构化配置（`MqttProperties`、`InfluxDbProperties`）
- 敏感配置通过环境变量注入
