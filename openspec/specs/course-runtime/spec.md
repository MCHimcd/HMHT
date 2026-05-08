# Course Runtime Specification

## Purpose

本规格记录当前课程版运行形态、统一响应、数据库迁移、启动种子数据和健康检查的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: Course Edition Runtime

系统 SHALL 以固定课程版形态运行，后端使用 Spring Boot、MySQL、Flyway、JPA 和业务 token，前端使用 Vue 3；当前代码不存在可切换的硬件、MQTT、InfluxDB、Python 或外部时序数据运行模式。

#### Scenario: Configuration Loads Course Edition Defaults

- **WHEN** 后端按默认配置启动
- **THEN** 应用名为 `HMHT`，`hmht.course.edition` 为 `true`，HTTP 端口为 `8080`，接口上下文路径为 `/api`
- **AND** MySQL 连接从 `HMHT_DB_URL`、`HMHT_DB_USER`、`HMHT_DB_PASSWORD` 读取，未设置时使用当前配置中的默认值
- **AND** JPA 使用 `validate`，Flyway 从 `classpath:db/migration` 执行迁移

#### Scenario: Security Filter Permits Requests

- **WHEN** HTTP 请求进入 Spring Security 过滤链
- **THEN** 当前安全配置关闭 CSRF、允许跨域并对所有请求执行 `permitAll`
- **AND** 需要登录或管理员身份的业务边界由 Service 层 token 校验完成

### Requirement: Unified API Response

系统 SHALL 使用统一响应体 `{ code, message, data }` 表达成功和业务错误。

#### Scenario: Successful Response

- **WHEN** Controller 通过 `ApiResponse.ok(data)` 返回业务数据
- **THEN** 响应体 `code` 为 `200`，`message` 为 `success`，`data` 为传入数据

#### Scenario: Empty Successful Response

- **WHEN** Controller 通过 `ApiResponse.ok()` 返回空成功结果
- **THEN** 响应体 `code` 为 `200`，`message` 为 `success`，`data` 为 `null`

#### Scenario: Business Error Response

- **WHEN** Controller 或 Service 抛出 `BizException`
- **THEN** 全局异常处理器返回 HTTP 200
- **AND** 响应体 `code` 和 `message` 保留业务异常中的值，`data` 为 `null`

### Requirement: Health Check

系统 SHALL 提供课程版服务健康检查。

#### Scenario: Query Health

- **WHEN** 客户端请求 `GET /api/v1/health`
- **THEN** 响应 `data.available` 为 `true`
- **AND** `data.application` 来自 `spring.application.name`
- **AND** `data.edition` 为 `course`
- **AND** `data.nodeId` 为 `SIM_Node_01`
- **AND** `data.dataSimulated` 为 `true`
- **AND** `data.time` 为服务端当前 `OffsetDateTime` 字符串

### Requirement: Flyway Course Schema

系统 SHALL 通过 `V1__course_schema.sql` 创建当前课程版保留的 MySQL 表结构。

#### Scenario: Initial Migration Creates Tables

- **WHEN** Flyway 执行初始迁移
- **THEN** 数据库包含 `users`、`nodes`、`sensor_type`、`node_sensor`、`equipment_type`、`node_equipment`、`alerts`、`verification_codes`、`scheduled_tasks`、`operation_logs`、`user_node_access`
- **AND** 唯一约束覆盖手机号、节点编号、传感器 key、设备 key、节点传感器绑定、节点设备绑定、用户节点授权和告警编号
- **AND** 查询索引覆盖节点归属、告警节点/处理状态、验证码手机号、任务用户/启用状态、操作日志类型/用户和用户节点授权

### Requirement: Essential Course Data

系统 SHALL 在启动时幂等补齐课程演示数据。

#### Scenario: Seed Users And Node

- **WHEN** `EssentialDataSeeder` 启动运行
- **THEN** 系统确保管理员账号 `13800000001` / `admin123` 和演示用户 `13800000000` / `123456` 存在
- **AND** 系统确保节点 `SIM_Node_01` / `课程模拟温室节点` 存在，归属于演示用户，`online=true`，`smartMode=false`
- **AND** 系统确保演示用户拥有 `SIM_Node_01` 的显式访问授权

#### Scenario: Seed Dictionaries And Bindings

- **WHEN** 种子流程检查传感器、设备和节点绑定
- **THEN** 系统确保传感器类型为 `temperature`、`humidity`、`light`、`soilHumidity`
- **AND** 系统确保设备类型为 `water_pump`、`led_light`、`fan`
- **AND** 系统确保 `SIM_Node_01` 绑定以上传感器和设备，并写入当前代码中的默认告警阈值

#### Scenario: Seed Demo Records

- **WHEN** 种子流程检查演示告警、任务和审计日志
- **THEN** 系统在缺失时创建 `COURSE_ALERT_1`
- **AND** 系统在演示用户没有任务时创建每日 `08:30` 的 `water_pump` / `turn_on` 任务
- **AND** 系统在操作日志为空时创建 `SYSTEM_SEED` 日志
