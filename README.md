# HMHT — 鸿蒙慧田智慧系统（后端）

> Spring Boot 后端服务，为鸿蒙慧田智慧农业 App（OpenHarmony）提供 REST API、MQTT 桥接和数据持久化。

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
│   ├── HmhtApplication.java          # 启动类 (@SpringBootApplication)
│   ├── web/                           # REST 控制器
│   ├── service/                       # 业务逻辑接口
│   ├── entity/                        # JPA 实体
│   ├── dto/                           # 数据传输对象
│   ├── repository/                    # Spring Data JPA 仓库
│   ├── mqtt/                          # MQTT 客户端（Paho）
│   ├── bridge/                        # MQTT/Python 桥接层
│   ├── influx/                        # InfluxDB 直接写入
│   ├── mock/                          # Mock 模式实现 & 种子数据
│   ├── config/                        # Spring 配置类
│   │   └── condition/                 # 运行模式条件注解
│   └── exception/                     # 全局异常处理
├── src/main/resources/
│   ├── application.properties         # 主配置文件
│   └── db/migration/                   # Flyway 迁移（V1 初始化 + V2 协议控制面）
├── src/test/java/ww/wangnong/         # 测试
├── scripts/
│   ├── start.ps1                      # 一键启动脚本（本地开发）
│   └── tests/
│       ├── start.ps1.test.ps1         # start.ps1 的脚本级测试入口
│       └── test-start.ps1             # start.ps1 的辅助测试套件
├── docs/deployment.md                 # 云部署指南
├── docs/backend-hardware-protocol.md  # 后端<->硬件通讯协议（V2）
├── docs/hardware-team-action-guide.md # 硬件成员执行指南（V2 对接）
└── .github/workflows/deploy.yml       # GitHub Actions 部署工作流
```

## 运行模式

HMHT 支持四种运行模式，通过 `hmht.run.mode` 属性（环境变量 `HMHT_RUN_MODE`）切换：

| 模式 | 说明 | 数据来源 |
|------|------|----------|
| **mock**（默认） | 仅 Java 服务，启动时自动播种演示数据 | MockDataLifecycle 内存种子 + InfluxDB 30 天历史 |
| **bridge** | App <-> Java <-> Python（不走硬件） | Python agri 后端 → MQTT → MqttBridge → App |
| **hardware** | App <-> Java <-> 硬件端（不走 Python） | 硬件设备 MQTT → MqttBridge → App |
| **full** | App <-> Java <-> Python <-> 硬件端（完整链路） | Python + 硬件双源 MQTT → MqttBridge → App |

### 运行模式条件装配

组件通过 `config/condition/` 包中的自定义条件注解进行条件装配：

| 注解 | 激活模式 | 说明 |
|------|----------|------|
| `@ConditionalOnMockMode` | mock | 仅 mock 模式激活 |
| `@ConditionalOnBridgeMode` | bridge/hardware/full | 非 mock 模式激活 |
| `@ConditionalOnPythonRoute` | bridge/full | Python 路由启用时激活 |
| `@ConditionalOnHardwareRoute` | hardware/full | 硬件路由启用时激活 |
| `@ConditionalOnNoPythonRoute` | mock/hardware | 无 Python 路由时激活 |

> 当前 V2 阶段能力：
> 1) App <-> 后端 <-> 硬件链路打通（telemetry/command/ack）；
> 2) 设备能力与影子查询接口（`/v1/devices/{deviceId}/capability|shadow`）；
> 3) 命令 ACK 超时重试闭环（默认 5s 超时、最多 3 次重试）；
> 4) 控制面持久化（request/ack/retry/audit/deadletter/shadow）。
>
> 后端<->硬件通讯接口协议与数据格式已独立维护，见 [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md)。

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HMHT_RUN_MODE` | `mock` | 运行模式：`mock` / `bridge` / `hardware` / `full` |
| `HMHT_MOCK_SEED_ENABLED` | `true` | mock 模式是否播种演示数据 |
| `HMHT_AGRI_CHAT_URL` | `http://localhost:5000` | Python agri chat 服务地址 |
| `HMHT_INFLUXDB_ENABLED` | `true` | 是否启用 InfluxDB |
| `HMHT_INFLUXDB_TOKEN` | （空） | InfluxDB 访问令牌 |
| `HMHT_COMMAND_RETRY_CHECK_INTERVAL_MS` | `5000` | ACK 超时扫描周期（毫秒） |
| `HMHT_COMMAND_RETRY_MAX_RETRIES` | `3` | 命令最大重试次数 |
| `HMHT_COMMAND_RETRY_DEFAULT_TIMEOUT_MS` | `5000` | 未指定 timeout_ms 时的默认超时 |

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

`scripts/start.ps1` 启动后会在主终端实时展示各服务状态（Starting/Running/Stopping/Stopped/Failed），并在 Ctrl+C 时执行统一停机：先优雅停止，超时后强制回收，确保标签页与子进程同步退出。

### 启动脚本测试

启动脚本的脚本级校验位于 `scripts/tests/`：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\tests\start.ps1.test.ps1
```

该测试用于验证 `scripts/start.ps1` 的语法、帮助信息、参数解析、编码和关键函数定义。

### 编译 JAR

```bash
./gradlew clean bootJar
```

输出：`build/libs/HMHT-0.0.1-SNAPSHOT.jar`

### 运行测试

```bash
./gradlew test
```

测试报告：`build/reports/tests/test/index.html`

#### 测试层次结构

| 层次 | 注解 | 文件数 | 说明 |
|------|------|--------|------|
| DTO / 异常 | `@Test` (纯 JUnit) | 3 | ApiResponse、BizException、GlobalExceptionHandler |
| 桥接与连接框架 | `@ExtendWith(MockitoExtension)` | 6 | TelemetryTranslator、MqttBridge、PahoMqttService、MqttSubscriptionRegistry、Influx 动态写读、ACK 重试 |
| Repository | `@DataJpaTest` (H2) | 11 | Repository 自定义查询与 V2 控制面仓库验证 |
| Service (Mock 实现) | `@ExtendWith(MockitoExtension)` | 10 | Mock 业务实现与能力聚合验证 |
| Controller | `@WebMvcTest` | 12 | 设备、节点、传感器、管理等端点验证 |
| 启动与基础 | `@SpringBootTest` / 工具测试 | 2 | 启动冒烟与测试辅助类 |

共 **44 个测试类**，覆盖 DTO → 桥接 → Repository → Service → Controller → 启动验证全链路。

#### 测试约定

- 命名规则：`{methodName}_{scenario}_{expectedResult}`
- 工厂：`TestDataFactory` 提供全部 13 个 Entity 的默认实例
- Mock 注入：Spring Boot 4.x 使用 `@MockitoBean`（非 `@MockBean`）
- 详细规范见 `.github/instructions/testing.instructions.md`

## REST API 端点清单

基路径：`/api`（`server.servlet.context-path=/api`）

### 认证 — AuthController (`/v1/auth`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/auth/login` | 用户登录 | 否 |
| POST | `/v1/auth/send-verification-code` | 发送验证码 | 否 |
| POST | `/v1/auth/register` | 用户注册 | 否 |
| POST | `/v1/auth/forgot-password` | 忘记密码 | 否 |
| GET | `/v1/auth/profile` | 获取个人资料 | Bearer Token |
| PUT | `/v1/auth/profile` | 更新个人资料 | Bearer Token |
| PUT | `/v1/auth/password` | 修改密码 | Bearer Token |
| POST | `/v1/auth/avatar/use-fixed` | 使用固定头像 | Bearer Token |
| POST | `/v1/auth/logout` | 登出 | Bearer Token |

### 节点管理 — NodeController (`/v1/nodes`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/nodes` | 获取用户节点列表 | Bearer Token |
| GET | `/v1/nodes/{nodeId}` | 获取节点详情 | Bearer Token |
| GET | `/v1/nodes/{nodeId}/sensor-meta` | 获取传感器元数据 | 否 |
| GET | `/v1/nodes/{nodeId}/equipment-meta` | 获取设备元数据 | 否 |
| PUT | `/v1/nodes/{nodeId}/sensor-thresholds` | 更新传感器阈值 | Bearer Token |

### 设备能力 — DeviceController (`/v1/devices`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/devices/{deviceId}/capability` | 获取设备能力快照（sensor/equipment/action） | 否 |
| GET | `/v1/devices/{deviceId}/shadow` | 获取设备影子快照（最近 ACK 状态） | 否 |

### 传感器 & 设备 — SensorController

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/sensors/{nodeId}/history` | 传感器历史数据（?metric=&range=） | 否 |
| POST | `/v1/equipment/stats` | 设备统计数据 | 否 |

### 告警管理 — AlertController (`/v1/alerts`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/alerts/list` | 查询告警列表 | 否 |
| POST | `/v1/alerts/handle` | 处理告警 | 否 |

### 定时任务 — ScheduledTaskController (`/v1/tasks`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/tasks` | 列出当前用户任务 | Bearer Token |
| POST | `/v1/tasks` | 创建任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}` | 更新任务 | Bearer Token |
| DELETE | `/v1/tasks/{taskId}` | 删除任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}/toggle` | 切换任务启用状态 | Bearer Token |

### AI 对话 — ChatController (`/v1/agri-qa`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/agri-qa/chat` | 发送消息（支持图像） | 否 |

### 溯源 & 摄像头 — ExtraController

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/traceability/scan` | 扫描溯源码 | 否 |
| GET | `/v1/cameras/{nodeId}/live` | 获取直播流 | 否 |
| GET | `/v1/cameras/{nodeId}/playback` | 获取回放列表 | 否 |
| GET | `/v1/cameras/{nodeId}/keyframes` | 获取关键帧列表 | 否 |

### 管理 — AdminController (`/v1/admin`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/admin/nodes` | 创建节点 | Bearer Token |
| PUT | `/v1/admin/nodes/{nodeId}` | 更新节点 | Bearer Token |
| DELETE | `/v1/admin/nodes/{nodeId}` | 删除节点 | Bearer Token |
| GET | `/v1/admin/users` | 列出所有用户 | Bearer Token |
| PUT | `/v1/admin/users/{userId}/role` | 更新用户角色 | Bearer Token |
| GET | `/v1/admin/users/{userId}/nodes` | 获取用户节点列表 | Bearer Token |
| PUT | `/v1/admin/users/{userId}/nodes` | 设置用户节点权限 | Bearer Token |

### 操作日志 — OperationLogController (`/v1/admin/logs`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/admin/logs` | 查询操作日志（?page=&size=&type=） | 否 |

### 健康检查 — HealthController (`/v1`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/health` | 前端可用性检查（统一 ApiResponse） | 否 |

### 基础设施 — InfraController (`/infra`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/infra/health` | 健康检查（含 MQTT 状态） | 否 |
| POST | `/infra/mqtt/publish` | 手动发布 MQTT 消息 | 否 |

## 数据模型清单

所有实体位于 `ww.wangnong.entity`，使用 Lombok `@Getter @Setter`。

| 实体 | 表名 | 主要字段 | 说明 |
|------|------|----------|------|
| `User` | `users` | phone(unique), passwordHash, userName, gender, birthDate, avatarSrc, role | 用户 |
| `Node` | `nodes` | nodeId(unique), nodeName, user(ManyToOne), online, smartMode | 农业节点 |
| `SensorType` | `sensor_type` | sensorKey(unique), name, unit, icon, badgeColor, bgColor | 传感器类型定义 |
| `NodeSensor` | `node_sensor` | nodeId, sensorKey(联合唯一), warningMin, warningMax | 节点传感器 + 阈值 |
| `EquipmentType` | `equipment_type` | equipmentKey(unique), name, icon, controllable | 设备类型定义 |
| `NodeEquipment` | `node_equipment` | nodeId, equipmentKey(联合唯一) | 节点设备绑定 |
| `Alert` | `alerts` | alertId(unique), nodeId, type, typeName, level, message, source, sourceName, isHandled, handleTime, alertTime | 告警记录 |
| `CommandLog` | `command_logs` | nodeId, userId, targetDevice, command, source | 命令日志 |
| `ChatMessage` | `chat_messages` | user(ManyToOne), role, content(TEXT), imageUri | 聊天消息 |
| `ScheduledTask` | `scheduled_tasks` | nodeId, userId, equipmentKey, action, taskName, hour, minute, repeatDays, enabled | 定时任务 |
| `OperationLog` | `operation_logs` | userId, userName, operationType, targetType, targetId, detail(TEXT), ipAddress | 操作审计日志 |
| `VerificationCode` | `verification_codes` | phone, code, expiresAt, used | 验证码 |
| `UserNodeAccess` | `user_node_access` | userId, nodeId(联合唯一) | 用户-节点访问权限 |
| `CommandRequest` | `command_request` | commandId(unique), deviceId, targetKey, action, status, traceId, timeoutMs, sentAt, ackedAt | 命令闭环主表（V2） |
| `CommandAck` | `command_ack` | commandId, deviceId, status, ackCode, ackMessage, traceId, ackedAt | 命令回执流水（V2） |
| `CommandRetryLog` | `command_retry_log` | commandId, attemptNo, retryReason, retryAt | 命令超时重试轨迹（V2） |
| `DeviceProfile` | `device_profile` | deviceId(unique), nodeId, displayName, hardwareModel, firmwareVersion, online, lastSeenAt | 设备档案（V2） |
| `DeviceCapability` | `device_capability` | deviceId, capabilityVersion, checksum, reportedAt | 设备能力快照（V2） |
| `CapabilitySensor` | `capability_sensor` | deviceId, capabilityVersion, sensorKey, dataType, unit, minValue, maxValue, requiredField | 传感器能力明细（V2） |
| `CapabilityEquipment` | `capability_equipment` | deviceId, capabilityVersion, equipmentKey, controllable, actionsJson | 设备能力明细（V2） |
| `DeviceShadow` | `device_shadow` | deviceId(unique), shadowJson, updatedAt | 设备状态影子（V2） |
| `IngestAudit` | `ingest_audit` | msgType, sourceTopic, deviceId, protocolVersion, traceId, status, errorMessage | 消息接入审计（V2） |
| `IngestDeadletter` | `ingest_deadletter` | msgType, sourceTopic, deviceId, traceId, reason, rawPayload | 消息死信队列（V2） |

## 服务清单

Service 接口定义在 `ww.wangnong.service`，根据运行模式由不同实现类提供。

| 接口 | Mock 实现 (`mock/`) | Bridge 实现 (`bridge/`) | 说明 |
|------|---------------------|------------------------|------|
| `AuthService` | `MockAuthService` | — | 认证 & 用户管理 |
| `NodeService` | `MockNodeService` | — | 节点 & 传感器元数据 |
| `AlertService` | `MockAlertService` | — | 告警查询 & 处理 |
| `SensorService` | `MockSensorService` | `InfluxSensorService` | 传感器历史 & 设备统计 |
| `DeviceService` | `DatabaseDeviceService`（统一实现） | — | 设备能力 & 设备影子查询 |
| `ChatService` | `MockChatService` | `AgriChatService` | AI 对话 |
| `ExtraService` | `MockExtraService` | — | 溯源 & 摄像头 |
| `AdminService` | `MockAdminService` | — | 管理员操作 |
| `ScheduledTaskService` | `MockScheduledTaskService` | — | 定时任务 CRUD |
| `OperationLogService`（实体类） | — | — | 操作日志（直接实现） |

**补充组件（非 Service 接口）：**

| 类 | 包 | 激活条件 | 说明 |
|----|----|----------|------|
| `RunMode` | `config/` | 始终 | 运行模式枚举定义 |
| `RunModeHolder` | `config/` | 始终 | 运行模式持有组件 |
| `MockDataLifecycle` | `mock/` | `@ConditionalOnMockMode` | mock 模式启动播种、关闭清理 |
| `MockMqttSimulator` | `mock/` | `@ConditionalOnMockMode` | mock 模式模拟 MQTT 遥测发布 |
| `MockAlertGenerator` | `mock/` | `@ConditionalOnMockMode` | mock 模式定时生成告警 |
| `MockTaskExecutor` | `mock/` | `@ConditionalOnMockMode` | mock 模式定时任务执行 |
| `MqttBridge` | `bridge/` | `@ConditionalOnBridgeMode` | Python ↔ App MQTT 双向桥接 |
| `TelemetryTranslator` | `bridge/` | 静态工具 | Python → App 遥测字段映射 |
| `InfluxTelemetryWriter` | `bridge/` | InfluxDB 启用 | 翻译后遥测写入 InfluxDB |
| `ProtocolPersistenceService` | `bridge/` | `@ConditionalOnBridgeMode` | V2 控制面持久化（命令请求、ACK、审计、死信） |
| `CommandAckTracker` | `bridge/` | `@ConditionalOnHardwareRoute` | ACK 超时扫描与命令重试（hardware/full 模式） |
| `InfluxTelemetryService` | `influx/` | InfluxDB 启用 | InfluxDB 低级写入（Line Protocol） |
| `PahoMqttService` | `mqtt/` | MQTT 启用 | Eclipse Paho MQTT 客户端封装（连接生命周期快照、重连重订阅） |
| `MqttSubscriptionRegistry` | `mqtt/` | 始终 | MQTT 订阅注册占位机制，统一管理 bridge 主题处理器 |

## Controller 清单

| Controller | 路径前缀 | 职责 |
|------------|----------|------|
| `AuthController` | `/v1/auth` | 登录、注册、验证码、密码、资料 |
| `NodeController` | `/v1/nodes` | 节点列表、详情、传感器/设备元数据、阈值 |
| `DeviceController` | `/v1/devices` | 设备能力快照与设备影子查询 |
| `SensorController` | `/v1/sensors`, `/v1/equipment` | 传感器历史数据、设备统计 |
| `AlertController` | `/v1/alerts` | 告警查询与处理 |
| `ScheduledTaskController` | `/v1/tasks` | 定时任务 CRUD |
| `ChatController` | `/v1/agri-qa` | AI 农业问答 |
| `ExtraController` | `/v1/traceability`, `/v1/cameras` | 溯源扫码、摄像头直播/回放/关键帧 |
| `AdminController` | `/v1/admin` | 节点管理、用户管理、权限分配 |
| `OperationLogController` | `/v1/admin/logs` | 操作日志查询 |
| `HealthController` | `/v1` | 前端可用性健康检查 |
| `InfraController` | `/infra` | 健康检查、MQTT 手动发布 |

## MQTT 协议

后端<->硬件通讯协议（V2）已独立维护，详见 [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md)。
本节保留当前 bridge 兼容主题与实现摘要（含历史 agri 链路）。

### 硬件直连主题（V2）

| 方向 | 主题 | 说明 |
|------|------|------|
| Device → HMHT | `/hmht/device/{deviceId}/telemetry` | 硬件设备遥测上报 |
| HMHT → Device | `/hmht/device/{deviceId}/command` | 后端下发设备控制命令 |
| Device → HMHT | `/hmht/device/{deviceId}/ack` | 硬件命令回执 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/ack` | 回执透传给 App |

### 历史兼容主题（agri）

| 方向 | 主题 | 说明 |
|------|------|------|
| Python → HMHT | `/agri/device/{deviceId}/telemetry` | Python 端遥测 |
| Python → HMHT | `/agri/ai_vision/alert` | AI 视觉告警 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/telemetry` | 翻译后遥测（App 订阅） |
| HMHT → App | `/wangnong_2026/node/{nodeId}/alert` | 翻译后告警（App 订阅） |
| App → HMHT → Python | `/wangnong_2026/node/{nodeId}/command` → `/agri/device/{deviceId}/command` | 设备控制命令转发 |
| Python → HMHT | `/agri/device/{deviceId}/ack` | 命令确认（仅日志） |

### 桥接逻辑（MqttBridge）

1. 通过 `MqttSubscriptionRegistry` 注册并绑定 7 个桥接主题：
	- `/agri/device/+/telemetry`
	- `/hmht/device/+/telemetry`
	- `/hmht/device/+/capability`
	- `/agri/ai_vision/alert`
	- `/wangnong_2026/node/+/command`
	- `/agri/device/+/ack`
	- `/hmht/device/+/ack`
2. 历史 agri 遥测通过 `TelemetryTranslator.pyToApp()` 转译后发布到 `/wangnong_2026/node/{nodeId}/telemetry`
3. 新硬件 V2 遥测通过 `TelemetryTranslator.hardwareV2ToApp()` 规范化后发布到 `/wangnong_2026/node/{nodeId}/telemetry`
4. App 命令同时转发到历史兼容 topic `/agri/device/{deviceId}/command` 与硬件直连 topic `/hmht/device/{deviceId}/command`
5. 硬件 ACK 从 `/hmht/device/{deviceId}/ack` 透传到 `/wangnong_2026/node/{nodeId}/ack`
6. AI 视觉告警解析置信度映射告警级别（≥0.85 critical，≥0.6 warning，其他 info）
7. 硬件 capability 从 `/hmht/device/{deviceId}/capability` 入库到 capability/profile 明细表
8. 遥测写入 InfluxDB：动态 measurement `telemetry_metric`（并兼容写入 legacy `environment`）
9. bridge 模式定时扫描超时命令：默认 5 秒超时、最多 3 次重试，超过阈值标记 `timeout`

### V2 兼容边界（兼容说明）

- 保持历史 agri 主题与 payload 解析兼容，App 侧无需修改订阅即可继续接收数据。
- 新增 hmht 主题能力（telemetry/capability/ack）通过后端桥接增量接入，不破坏旧链路。
- 动态 sensor/equipment key 允许透传和落库，查询层优先走新时序模型并保留 legacy 回退。

### 遥测字段映射（TelemetryTranslator）

| Python 字段 | App 字段 |
|-------------|----------|
| `air_temp` | `temperature` |
| `air_humidity` | `humidity` |
| `soil_temp` | `soilTemp` |
| `soil_moisture` | `soilHumidity` |
| `soil_ph` | `soilPh` |
| `light_intensity` | `light` |
| `co2_ppm` | `co2` |
| `soil_n` | `soilN` |
| `soil_p` | `soilP` |
| `soil_k` | `soilK` |

## 与 agri 项目的协作

在 **bridge 模式**下，HMHT 后端与 agri 项目的 Python 服务配合工作：

| Python 服务 | 文件 | 职责 | HMHT 交互方式 |
|-------------|------|------|---------------|
| 后端主进程 | `backend_main_debug.py` | MQTT 订阅硬件遥测、SunAgent 智能决策 | MQTT 桥接（MqttBridge） |
| Web 服务 | `web_server_debug.py` | Flask API（含多模态 AI 对话） | HTTP 代理（AgriChatService → `POST /api/multi-conversation/chat`） |
| 模拟设备 | `mock_device.py` | 模拟硬件 MQTT 遥测发布 | MQTT 桥接（MqttBridge） |

**AgriChatService** 通过 `RestClient` 将 App 端的 AI 对话请求代理到 Python Flask 服务（multipart 表单，支持图片上传），并维护 `userId → sessionId` 会话映射。

## 硬件团队对接

- 协议规范：`docs/backend-hardware-protocol.md`
- 执行清单：`docs/hardware-team-action-guide.md`

## 数据库迁移

- 迁移目录：`src/main/resources/db/migration/`
- 当前版本：`V2__device_protocol_control_plane.sql`（在 V1 基础上新增协议控制面 10 张表）
- Flyway 命名规范：`V{N}__{description}.sql`（双下划线分隔）
- JPA DDL 策略：`spring.jpa.hibernate.ddl-auto=update`（Flyway 创建基础表，Hibernate 补充变更）

## 安全架构

- **SecurityConfig** 当前配置：所有请求放行（`authorizeHttpRequests → anyRequest().permitAll()`）
- CORS 全开放（`allowedOrigins("*")`）
- CSRF 已禁用
- Token 提取方式：Controller 手动从 `Authorization: Bearer {token}` 头提取，传递给 Service 层做业务级校验

## 部署

详见 [docs/deployment.md](docs/deployment.md)。概要：

- **CI/CD**：GitHub Actions → SSH → systemd 服务重启
- **触发方式**：手动触发（`workflow_dispatch`）
- **构建**：`./gradlew clean bootJar -x test`
- **运行时**：Eclipse Temurin JRE 21
- **服务管理**：systemd unit file

## 设计约定

### 统一响应格式

所有 API 使用 `ApiResponse<T>` 封装：

```json
{ "code": 200, "message": "success", "data": ... }
```

错误响应：

```json
{ "code": 400, "message": "错误描述", "data": null }
```

### 全局异常处理

- `BizException(code, message)` — 业务异常，由 `GlobalExceptionHandler` 捕获并转为 `ApiResponse.error()`
- 非 BizException 由 Spring 默认处理

### 配置管理

- `@ConfigurationProperties` 用于结构化配置（`MqttProperties`、`InfluxDbProperties`）
- `@ConditionalOnProperty` / `@ConditionalOnExpression` 用于运行模式切换
- 敏感配置通过环境变量注入（数据库密码、InfluxDB Token）
