# HMHT — 万农智慧系统（后端）

> Spring Boot 后端服务，为万农智慧农业 App（HarmonyOS）提供 REST API、MQTT 桥接和数据持久化。

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
| 容器化 | Docker + Docker Compose | — |
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
│   └── exception/                     # 全局异常处理
├── src/main/resources/
│   ├── application.properties         # 主配置文件
│   └── db/migration/V1__init.sql      # Flyway 初始化脚本
├── src/test/java/ww/wangnong/         # 测试
├── docker-compose.yml                 # 容器编排（全链路）
├── Dockerfile                         # Spring Boot JAR 镜像
├── start-all.ps1                      # 一键启动脚本（本地开发）
├── docs/deployment.md                 # 云部署指南
└── .github/workflows/deploy.yml       # GitHub Actions 部署工作流
```

## 运行模式

HMHT 支持两种运行模式，通过 `hmht.run.mode` 属性（环境变量 `HMHT_RUN_MODE`）切换：

| 模式 | 说明 | 数据来源 |
|------|------|----------|
| **mock**（默认） | 仅 Java 服务，启动时自动播种演示数据 | MockDataLifecycle 内存种子 + InfluxDB 30 天历史 |
| **bridge** | Java + Python 全链路，MQTT 双向桥接 | Python agri 后端 → MQTT → MqttBridge → App |

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HMHT_RUN_MODE` | `mock` | 运行模式：`mock` / `bridge` |
| `HMHT_MOCK_SEED_ENABLED` | `true` | mock 模式是否播种演示数据 |
| `HMHT_AGRI_CHAT_URL` | `http://localhost:5000` | Python agri chat 服务地址 |
| `HMHT_INFLUXDB_ENABLED` | `true` | 是否启用 InfluxDB |
| `HMHT_INFLUXDB_TOKEN` | （空） | InfluxDB 访问令牌 |

## 构建与运行

### 本地开发（mock 模式）

```bash
./gradlew bootRun
```

### 本地开发（全链路）

```powershell
.\start-all.ps1 -Mode bridge
```

支持参数：`-Mode mock|bridge`、`-PythonDir`、`-JavaDir`、`-AgriChatPort`、`-InfluxDir`、`-InfluxToken`。自动检测 Windows Terminal 多标签。

### Docker Compose（全链路）

```bash
docker compose up -d
```

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
| 桥接工具 | `@ExtendWith(MockitoExtension)` | 1 | TelemetryTranslator 键名翻译 |
| Repository | `@DataJpaTest` (H2) | 7 | 13 个 Repository 的自定义查询方法 |
| Service (Mock 实现) | `@ExtendWith(MockitoExtension)` | 9 | 8 个 Mock 实现 + OperationLogService |
| Controller | `@WebMvcTest` | 10 | 10 个 Controller 端点验证 |

共 **31 个测试类**，覆盖 DTO → Repository → Service → Controller 全链路。

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

## 服务清单

Service 接口定义在 `ww.wangnong.service`，根据运行模式由不同实现类提供。

| 接口 | Mock 实现 (`mock/`) | Bridge 实现 (`bridge/`) | 说明 |
|------|---------------------|------------------------|------|
| `AuthService` | `MockAuthService` | — | 认证 & 用户管理 |
| `NodeService` | `MockNodeService` | — | 节点 & 传感器元数据 |
| `AlertService` | `MockAlertService` | — | 告警查询 & 处理 |
| `SensorService` | `MockSensorService` | `InfluxSensorService` | 传感器历史 & 设备统计 |
| `ChatService` | `MockChatService` | `AgriChatService` | AI 对话 |
| `ExtraService` | `MockExtraService` | — | 溯源 & 摄像头 |
| `AdminService` | `MockAdminService` | — | 管理员操作 |
| `ScheduledTaskService` | `MockScheduledTaskService` | — | 定时任务 CRUD |
| `OperationLogService`（实体类） | — | — | 操作日志（直接实现） |

**补充组件（非 Service 接口）：**

| 类 | 包 | 说明 |
|----|----|------|
| `MockDataLifecycle` | `mock/` | mock 模式启动播种、关闭清理 |
| `MockMqttSimulator` | `mock/` | mock 模式模拟 MQTT 遥测发布 |
| `MockAlertGenerator` | `mock/` | mock 模式定时生成告警 |
| `MockTaskExecutor` | `mock/` | mock 模式定时任务执行 |
| `MqttBridge` | `bridge/` | Python ↔ App MQTT 双向桥接 |
| `TelemetryTranslator` | `bridge/` | Python → App 遥测字段映射 |
| `InfluxTelemetryWriter` | `bridge/` | 翻译后遥测写入 InfluxDB |
| `InfluxTelemetryService` | `influx/` | InfluxDB 低级写入（Line Protocol） |
| `PahoMqttService` | `mqtt/` | Eclipse Paho MQTT 客户端封装 |

## Controller 清单

| Controller | 路径前缀 | 职责 |
|------------|----------|------|
| `AuthController` | `/v1/auth` | 登录、注册、验证码、密码、资料 |
| `NodeController` | `/v1/nodes` | 节点列表、详情、传感器/设备元数据、阈值 |
| `SensorController` | `/v1/sensors`, `/v1/equipment` | 传感器历史数据、设备统计 |
| `AlertController` | `/v1/alerts` | 告警查询与处理 |
| `ScheduledTaskController` | `/v1/tasks` | 定时任务 CRUD |
| `ChatController` | `/v1/agri-qa` | AI 农业问答 |
| `ExtraController` | `/v1/traceability`, `/v1/cameras` | 溯源扫码、摄像头直播/回放/关键帧 |
| `AdminController` | `/v1/admin` | 节点管理、用户管理、权限分配 |
| `OperationLogController` | `/v1/admin/logs` | 操作日志查询 |
| `InfraController` | `/infra` | 健康检查、MQTT 手动发布 |

## MQTT 协议

### 主题约定

| 方向 | 主题 | 说明 |
|------|------|------|
| Python → HMHT | `/agri/device/{deviceId}/telemetry` | Python 端遥测 |
| Python → HMHT | `/agri/ai_vision/alert` | AI 视觉告警 |
| HMHT → App | `/wangnong_2026/node/{nodeId}/telemetry` | 翻译后遥测（App 订阅） |
| HMHT → App | `/wangnong_2026/node/{nodeId}/alert` | 翻译后告警（App 订阅） |
| App → HMHT → Python | `/wangnong_2026/node/{nodeId}/command` → `/agri/device/{deviceId}/command` | 设备控制命令转发 |
| Python → HMHT | `/agri/device/{deviceId}/ack` | 命令确认（仅日志） |

### 桥接逻辑（MqttBridge）

1. 订阅 Python 端 `/agri/device/+/telemetry` 和 `/agri/ai_vision/alert`
2. 通过 `TelemetryTranslator` 翻译字段名（如 `air_temp` → `temperature`）
3. 发布到 App 端主题 `/wangnong_2026/node/{nodeId}/telemetry`
4. AI 视觉告警解析置信度映射告警级别（≥0.85 critical，≥0.6 warning，其他 info）
5. 同时写入 InfluxDB（通过 `InfluxTelemetryWriter`）

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
| `soil_n` | `soil_n` |
| `soil_p` | `soil_p` |
| `soil_k` | `soil_k` |

## 与 agri 项目的协作

在 **bridge 模式**下，HMHT 后端与 agri 项目的 Python 服务配合工作：

| Python 服务 | 文件 | 职责 | HMHT 交互方式 |
|-------------|------|------|---------------|
| 后端主进程 | `backend_main_debug.py` | MQTT 订阅硬件遥测、SunAgent 智能决策 | MQTT 桥接（MqttBridge） |
| Web 服务 | `web_server_debug.py` | Flask API（含多模态 AI 对话） | HTTP 代理（AgriChatService → `POST /api/multi-conversation/chat`） |
| 模拟设备 | `mock_device.py` | 模拟硬件 MQTT 遥测发布 | MQTT 桥接（MqttBridge） |

**AgriChatService** 通过 `RestClient` 将 App 端的 AI 对话请求代理到 Python Flask 服务（multipart 表单，支持图片上传），并维护 `userId → sessionId` 会话映射。

## 数据库迁移

- 迁移目录：`src/main/resources/db/migration/`
- 当前版本：`V1__init.sql`（13 张表初始化）
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
- `@ConditionalOnProperty` 用于 mock/bridge 模式切换
- 敏感配置通过环境变量注入（数据库密码、InfluxDB Token）