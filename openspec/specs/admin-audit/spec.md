# Admin Audit Specification

## Purpose

本规格记录当前管理员节点管理、用户管理、节点授权和操作日志查询的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: Admin Node Management

系统 SHALL 只允许管理员执行业务层节点管理操作。

#### Scenario: Create Node

- **WHEN** 管理员请求 `POST /api/v1/admin/nodes`
- **AND** 请求体包含 `nodeId` 和 `nodeName`
- **THEN** 系统在节点不存在时创建节点
- **AND** 新节点归属于当前管理员，`online=false`，`smartMode=false`
- **AND** 系统写入 `NODE_CREATE` 操作日志
- **AND** 节点已存在时返回业务错误 `409` / `节点已存在`

#### Scenario: Update Node

- **WHEN** 管理员请求 `PUT /api/v1/admin/nodes/{nodeId}`
- **THEN** 系统在节点存在时按传入的非 `null` `nodeName` 更新节点名称
- **AND** 系统写入 `NODE_UPDATE` 操作日志
- **AND** 节点不存在时返回业务错误 `404` / `节点不存在`

#### Scenario: Delete Node

- **WHEN** 管理员请求 `DELETE /api/v1/admin/nodes/{nodeId}`
- **THEN** 系统在节点存在时删除该节点
- **AND** 删除节点前清理该 `nodeId` 对应的 `node_sensor` 和 `node_equipment` 绑定
- **AND** 系统写入 `NODE_DELETE` 操作日志
- **AND** 节点不存在时返回业务错误 `404` / `节点不存在`

### Requirement: Admin User Management

系统 SHALL 只允许管理员查询用户、修改用户角色和维护用户节点授权。

#### Scenario: List Users

- **WHEN** 管理员请求 `GET /api/v1/admin/users`
- **THEN** 系统返回所有用户的 `id`、`userName`、`phone`、`role`

#### Scenario: Update User Role

- **WHEN** 管理员请求 `PUT /api/v1/admin/users/{userId}/role`
- **AND** 请求体包含 `role`
- **THEN** 系统按传入值覆盖用户角色
- **AND** 系统写入 `ROLE_CHANGE` 操作日志
- **AND** 用户不存在时返回业务错误 `404` / `用户不存在`

#### Scenario: Get User Node Grants

- **WHEN** 管理员请求 `GET /api/v1/admin/users/{userId}/nodes`
- **THEN** 系统返回该用户在 `user_node_access` 中的 `nodeId` 列表

#### Scenario: Replace User Node Grants

- **WHEN** 管理员请求 `PUT /api/v1/admin/users/{userId}/nodes`
- **AND** 请求体包含 `nodeIds`
- **THEN** 系统先删除该用户旧授权
- **AND** 再逐条写入传入的节点 ID
- **AND** 系统写入 `NODE_UPDATE` 操作日志，目标类型为 `user_node_access`

#### Scenario: Non Admin Rejected

- **WHEN** 非管理员调用管理员 Service 能力
- **THEN** 系统返回业务错误 `403` / `需要管理员权限`

### Requirement: Operation Logging

系统 SHALL 保存业务操作日志并提供分页筛选查询。

#### Scenario: Write Operation Log

- **WHEN** 业务服务调用 `OperationLogService.log`
- **THEN** 系统保存 `operationType`、`userId`、`userName`、`targetType`、`targetId`、`detail`
- **AND** `createdAt` 由数据库或 ORM 创建时间写入

#### Scenario: Query Operation Logs

- **WHEN** 客户端请求 `GET /api/v1/admin/logs`
- **THEN** 系统按 `createdAt` 倒序返回日志分页
- **AND** `page` 最小按第 1 页处理
- **AND** `size` 最小为 1，最大为 100
- **AND** 响应包含 `total` 和 `items`

#### Scenario: Filter Operation Logs

- **WHEN** 请求参数包含非空 `type`
- **THEN** 系统按 `operationType` 精确筛选
- **WHEN** 请求参数包含非空 `targetId`
- **THEN** 系统按 `targetId` 精确筛选
- **WHEN** 请求参数包含 `startDate`
- **THEN** 系统按该日期当天零点作为开始时间筛选
- **WHEN** 请求参数包含 `endDate`
- **THEN** 系统按该日期最后一刻作为结束时间筛选

#### Scenario: Log Query Has No Auth Header Requirement

- **WHEN** 客户端请求 `GET /api/v1/admin/logs`
- **THEN** 当前 Controller 不读取 `Authorization` 请求头
- **AND** 当前实现不在该接口执行管理员校验
