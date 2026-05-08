# Scheduled Tasks Specification

## Purpose

本规格记录当前定时任务查询、创建、更新、删除和启停切换的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: Task Listing

系统 SHALL 按当前登录用户列出定时任务。

#### Scenario: List Current User Tasks

- **WHEN** 用户携带有效 token 请求 `GET /api/v1/tasks`
- **THEN** 系统只读取 `user_id` 等于当前用户 ID 的任务
- **AND** 任务按 `createdAt` 倒序返回
- **AND** 每个任务包含 `id`、`nodeId`、`equipmentKey`、`action`、`taskName`、`hour`、`minute`、`repeatDays`、`enabled`、`createdAt`

### Requirement: Task Creation

系统 SHALL 为当前用户保存定时任务定义。

#### Scenario: Create Task

- **WHEN** 用户携带有效 token 请求 `POST /api/v1/tasks`
- **AND** 请求体包含 `nodeId`、`equipmentKey`、`action`、`taskName`、`hour`、`minute`、`repeatDays`
- **THEN** 系统创建任务并将 `userId` 设置为当前用户 ID
- **AND** 新任务 `enabled=true`
- **AND** 系统写入 `TASK_CREATE` 操作日志
- **AND** 响应创建后的任务信息

### Requirement: Task Modification

系统 SHALL 允许任务拥有者或管理员修改、删除和启停任务。

#### Scenario: Update Task

- **WHEN** 用户请求 `PUT /api/v1/tasks/{taskId}`
- **AND** 任务存在且用户是任务拥有者或管理员
- **THEN** 系统覆盖任务的节点、设备、动作、名称、时分和重复日期字段
- **AND** 系统写入 `TASK_UPDATE` 操作日志
- **AND** 响应更新后的任务信息

#### Scenario: Delete Task

- **WHEN** 用户请求 `DELETE /api/v1/tasks/{taskId}`
- **AND** 任务存在且用户是任务拥有者或管理员
- **THEN** 系统删除该任务
- **AND** 系统写入 `TASK_DELETE` 操作日志
- **AND** 响应为空成功结果

#### Scenario: Toggle Task

- **WHEN** 用户请求 `PUT /api/v1/tasks/{taskId}/toggle`
- **AND** 任务存在且用户是任务拥有者或管理员
- **THEN** 系统将 `enabled` 取反并保存
- **AND** 系统写入 `TASK_UPDATE` 操作日志
- **AND** 响应为空成功结果

#### Scenario: Task Permission Errors

- **WHEN** 任务不存在
- **THEN** 系统返回业务错误 `404` / `任务不存在`
- **AND** 用户不是任务拥有者且不是管理员时返回业务错误 `403` / `无权操作`

### Requirement: Task Definition Only

系统 SHALL 只维护任务配置数据。

#### Scenario: Store Task Without Device Command

- **WHEN** 用户创建、更新、删除或切换任务
- **THEN** 当前实现只读写 `scheduled_tasks`
- **AND** 当前实现不会向真实设备、MQTT 或硬件通道发送命令
