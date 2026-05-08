# Alerts Specification

## Purpose

本规格记录当前告警写入、告警查询、筛选、分页和处理的已实现行为，包括近实时保留上限、启动场景查询、字段兼容、状态计数和审计日志。它只描述现有代码，不引入新需求，也不要求后端增加新的告警来源或权限边界。

## Requirements

### Requirement: Alert Storage

系统 SHALL 将告警保存为有限窗口，而不是无限历史。

#### Scenario: Add Alert Below Limit

- **WHEN** 服务调用 `AlertService.addAlert`
- **AND** 当前告警总数小于 200
- **THEN** 系统按传入 `AlertInfo` 创建 `alerts` 记录
- **AND** `alertTime` 使用传入毫秒时间戳按系统时区转换
- **AND** 传入的已处理告警会同步写入 `handleTime`

#### Scenario: Add Alert At Limit

- **WHEN** 服务调用 `AlertService.addAlert`
- **AND** 当前告警总数大于或等于 200
- **THEN** 系统先按 `createdAt` 从旧到新删除超出窗口所需数量的旧告警
- **AND** 然后保存新告警

### Requirement: Alert Query

系统 SHALL 通过 `POST /api/v1/alerts/list` 查询告警，并兼容当前前端请求字段。

#### Scenario: Normalize Query Size

- **WHEN** 请求体 `query` 中没有 `pageSize` 但有 `size`
- **THEN** Controller 将 `size` 复制为 `pageSize`

#### Scenario: Default Pagination

- **WHEN** 查询没有提供有效 `page` 或 `pageSize`
- **THEN** Service 使用 `page=1` 和 `pageSize=20`
- **AND** `page` 与 `pageSize` 至少为 `1`

#### Scenario: Startup Scene

- **WHEN** `query.scene` 为 `startup`
- **THEN** 系统优先返回所有未处理告警
- **AND** 追加最近 10 条已处理告警

#### Scenario: Normal Scene

- **WHEN** `query.scene` 不是 `startup`
- **THEN** 系统按 `createdAt` 倒序读取全部告警并转换为响应项

#### Scenario: Apply Filters

- **WHEN** `filter_type` 非空且不等于 `all`
- **THEN** 系统只保留 `type` 等于该值的告警
- **AND** `node_id` 或 `nodeId` 非空时只保留对应节点告警
- **AND** `filter_status=handled` 时只保留已处理告警
- **AND** `filter_status=unhandled` 时只保留未处理告警

#### Scenario: Count Before Status Filter

- **WHEN** 系统计算 `unhandled_count` 和 `handled_recent_count`
- **THEN** 计数基于类型和节点过滤后的集合
- **AND** 计数发生在 `filter_status` 过滤之前
- **AND** `handled_recent_count` 不超过 `10`

#### Scenario: Paginate Alert Items

- **WHEN** 系统完成过滤并分页
- **THEN** `total` 为状态过滤后的总数
- **AND** `items` 为当前页区间内的告警列表
- **AND** 响应字段使用 `snake_case`

### Requirement: Alert Handling

系统 SHALL 将告警处理为已处理状态并记录审计日志。

#### Scenario: Handle Alert

- **WHEN** 客户端请求 `POST /api/v1/alerts/handle`
- **THEN** Controller 从 `data.alert_id`、`data.alertId` 或顶层字段解析告警 ID
- **AND** `action` 缺省为 `handle`
- **AND** Service 将告警 `isHandled` 设置为 `true`，`handleTime` 设置为当前时间
- **AND** 系统写入 `ALERT_HANDLE` 操作日志，日志详情为传入 `action`
- **AND** 响应为空成功结果

#### Scenario: Handle Missing Alert

- **WHEN** 告警 ID 不存在
- **THEN** 系统返回业务错误 `404` / `告警不存在`
