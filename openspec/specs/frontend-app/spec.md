# Frontend App Specification

## Purpose

本规格记录当前 Vue 3 前端的 API 客户端、登录态、路由守卫、页面路由和课程域固定选项的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: API Client

前端 SHALL 通过统一客户端调用后端 API，并解包 `{ code, message, data }` 响应。

#### Scenario: Build Request

- **WHEN** 前端调用 `request(path, options)`
- **THEN** 请求 URL 为 `apiBaseUrl + path`
- **AND** `apiBaseUrl` 来自 `VITE_API_BASE_URL`，未设置时为 `/api`
- **AND** 请求头包含 `Accept: application/json`
- **AND** 请求体存在时设置 `Content-Type: application/json` 并执行 JSON 序列化
- **AND** 内存中存在 token 时添加 `Authorization: Bearer {token}`

#### Scenario: Handle Successful API Response

- **WHEN** 后端返回可解析 JSON 且响应体 `code` 为 `200`
- **THEN** 客户端返回响应体中的 `data`

#### Scenario: Handle API Error

- **WHEN** 后端响应体 `code` 不为 `200`
- **THEN** 客户端抛出 `ApiError(code, message)`
- **AND** `code` 为 `401` 时触发已注册的未授权处理器

#### Scenario: Handle Transport Or Format Error

- **WHEN** `fetch` 调用失败
- **THEN** 客户端抛出 `ApiError(0, "服务不可用，请确认后端已启动")`
- **WHEN** 响应体无法解析为 JSON
- **THEN** 客户端抛出 `ApiError(response.status, "响应格式异常")`

### Requirement: Auth Store

前端 SHALL 在 Pinia 中维护业务 token、过期时间和用户资料缓存。

#### Scenario: Persist Login State

- **WHEN** 登录成功
- **THEN** 前端保存 `hmht.token`、`hmht.tokenExpiresAt` 和 `hmht.profile` 到 `localStorage`
- **AND** `tokenExpiresAt` 使用当前时间加后端 `expires_in` 秒计算
- **AND** API 客户端同步绑定当前 token

#### Scenario: Restore Login State

- **WHEN** 应用启动恢复登录态
- **THEN** 前端从 `localStorage` 读取 token、过期时间和资料缓存
- **AND** 缓存过期时清空登录态
- **AND** 有 token 时调用 `getProfile` 刷新资料
- **AND** 非网络类 API 错误会清空登录态

#### Scenario: Clear Login State

- **WHEN** 用户登出、token 过期或收到 401 业务错误
- **THEN** 前端清空内存状态
- **AND** 删除 `hmht.token`、`hmht.tokenExpiresAt`、`hmht.profile`
- **AND** API 客户端清空 Authorization token

### Requirement: Route Guard

前端 SHALL 使用路由元信息控制登录页、业务页和管理员页访问。

#### Scenario: Require Authentication

- **WHEN** 用户访问带 `requiresAuth` 的路由且未登录
- **THEN** 前端重定向到 `/login`
- **AND** 查询参数 `redirect` 保留原始目标路径

#### Scenario: Authenticated User Opens Auth Page

- **WHEN** 已登录用户访问 `/login`、`/register` 或 `/forgot-password`
- **THEN** 前端重定向到 `/home`

#### Scenario: Require Admin

- **WHEN** 非管理员访问带 `requiresAdmin` 的路由
- **THEN** 前端重定向到 `/home`

### Requirement: Route Map

前端 SHALL 提供当前代码中的课程页面路由和兼容重定向。

#### Scenario: Main Routes

- **WHEN** 用户访问课程前端
- **THEN** `/` 重定向到 `/home`
- **AND** 已实现页面包括 `/home`、`/gateway`、`/nodes/:nodeId`、`/nodes/:nodeId/templates`、`/nodes/:nodeId/control`、`/nodes/:nodeId/realtime`、`/nodes/:nodeId/history`、`/nodes/:nodeId/equipment/stats`、`/strategies`、`/tasks`、`/alerts`、`/profile`、`/admin`、`/admin/logs`

#### Scenario: Compatibility Redirects

- **WHEN** 用户访问 `/dashboard`
- **THEN** 前端重定向到 `/home`
- **WHEN** 用户访问 `/nodes/:nodeId/sensors`
- **THEN** 前端重定向到 `/nodes/:nodeId/history`
- **WHEN** 用户访问 `/nodes/:nodeId/equipment`
- **THEN** 前端重定向到 `/nodes/:nodeId/control`
- **WHEN** 用户访问未知路径
- **THEN** 前端重定向到 `/`

### Requirement: Course Domain Options

前端 SHALL 使用当前固定课程节点、传感器、时间范围和设备选项。

#### Scenario: Default Course Node

- **WHEN** 页面需要默认节点
- **THEN** 前端使用 `SIM_Node_01`

#### Scenario: Sensor And Range Options

- **WHEN** 页面展示传感器和历史范围选择
- **THEN** 传感器选项为 `temperature`、`humidity`、`light`、`soilHumidity`
- **AND** 历史范围选项为 `24h`、`7d`、`30d`

#### Scenario: Equipment Options

- **WHEN** 页面展示设备操作选项
- **THEN** 设备选项为 `water_pump`、`led_light`、`fan`
- **AND** 前端使用当前代码中的中文标签和开关文案

### Requirement: Frontend Request Contracts

前端 SHALL 使用当前已实现的请求封装调用后端功能。

#### Scenario: Use Implemented API Modules

- **WHEN** 页面调用认证、节点、传感器、告警、任务、管理员或日志功能
- **THEN** 前端通过 `src/api/auth.ts`、`nodes.ts`、`sensors.ts`、`alerts.ts`、`tasks.ts`、`admin.ts` 中的函数发送请求
- **AND** 告警请求同时发送 `nodeId` 和 `node_id`
- **AND** 设备统计请求发送 `node_id` 和 `equipment_key`

#### Scenario: Validate Task Form

- **WHEN** 前端校验任务表单
- **THEN** 任务名称长度必须为 2 到 30 个字符
- **AND** 小时必须在 0 到 23
- **AND** 分钟必须在 0 到 59
- **AND** `repeatDays` 不可为空
