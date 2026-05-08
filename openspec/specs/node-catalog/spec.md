# Node Catalog Specification

## Purpose

本规格记录当前节点列表、节点详情、传感器元数据、设备元数据和阈值更新的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: Node Visibility

系统 SHALL 基于当前用户角色、节点归属和显式授权返回可见节点。

#### Scenario: List Nodes As Admin

- **WHEN** 管理员携带有效 token 请求 `GET /api/v1/nodes`
- **THEN** 系统返回数据库中的全部节点
- **AND** 每个节点包含 `id`、`name`、`status`
- **AND** `status` 根据节点 `online` 字段转换为 `online` 或 `offline`

#### Scenario: List Nodes As User

- **WHEN** 普通用户携带有效 token 请求 `GET /api/v1/nodes`
- **THEN** 系统返回该用户拥有的节点和 `user_node_access` 中显式授权的节点
- **AND** 同一 `nodeId` 的重复项会按首次出现去重

### Requirement: Node Detail

系统 SHALL 对节点详情执行业务访问控制。

#### Scenario: Get Accessible Node

- **WHEN** 用户携带有效 token 请求 `GET /api/v1/nodes/{nodeId}` 且拥有访问权
- **THEN** 系统返回节点 `id`、`name`、`status`
- **AND** `nodeState.smartMode` 来自节点 `smartMode` 字段

#### Scenario: Node Detail Errors

- **WHEN** 请求的节点不存在
- **THEN** 系统返回业务错误 `404` / `节点不存在`
- **AND** 节点存在但用户不是管理员、不是拥有者、也没有显式授权时返回业务错误 `403` / `无权访问该节点`

### Requirement: Sensor Metadata

系统 SHALL 从节点传感器绑定和全局传感器字典组装传感器元数据。

#### Scenario: Get Sensor Metadata

- **WHEN** 客户端请求 `GET /api/v1/nodes/{nodeId}/sensor-meta`
- **THEN** 系统读取该节点的 `node_sensor` 绑定
- **AND** 对每个绑定按 `sensor_key` 查询 `sensor_type`
- **AND** 返回 `key`、`name`、`unit`、`icon`、`badgeColor`、`bgColor`、`warningMin`、`warningMax`
- **AND** 缺失字典项的绑定不会出现在响应中

### Requirement: Equipment Metadata

系统 SHALL 从节点设备绑定和全局设备字典组装设备元数据。

#### Scenario: Get Equipment Metadata

- **WHEN** 客户端请求 `GET /api/v1/nodes/{nodeId}/equipment-meta`
- **THEN** 系统读取该节点的 `node_equipment` 绑定
- **AND** 对每个绑定按 `equipment_key` 查询 `equipment_type`
- **AND** 返回 `key`、`name`、`icon`、`controllable`、`controlType`
- **AND** 字典中的 `controlType` 为 `null` 时返回 `toggle`
- **AND** 缺失字典项的绑定不会出现在响应中

### Requirement: Sensor Thresholds

系统 SHALL 允许有节点访问权的用户更新节点传感器阈值。

#### Scenario: Update Thresholds

- **WHEN** 用户携带有效 token 请求 `PUT /api/v1/nodes/{nodeId}/sensor-thresholds`
- **AND** 请求体是 `ThresholdUpdate` 数组
- **THEN** 系统先校验节点存在和用户访问权
- **AND** 对每个条目按 `nodeId + sensorKey` 查找 `node_sensor`
- **AND** 系统保存传入的 `warningMin` 和 `warningMax`
- **AND** 系统写入 `THRESHOLD_UPDATE` 操作日志
- **AND** 响应为空成功结果

#### Scenario: Threshold Update Errors

- **WHEN** 节点不存在
- **THEN** 系统返回业务错误 `404` / `节点不存在`
- **AND** 用户无权访问节点时返回业务错误 `403` / `无权访问该节点`
- **AND** 节点传感器绑定不存在时返回业务错误 `404` / `传感器不存在: {sensorKey}`
