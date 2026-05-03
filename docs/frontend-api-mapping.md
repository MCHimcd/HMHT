# Frontend API Mapping

本文把当前 Vue 3 页面映射到后端 REST API。接口细节以 [api-endpoints.md](api-endpoints.md) 为准，本文侧重前端联调和请求封装。

## Shared Rules

| Item | Value |
|------|-------|
| Backend default | `http://localhost:8080` |
| API base path | `/api` |
| Frontend local base | `/api` through Vite proxy |
| Success response | `{ "code": 200, "message": "success", "data": ... }` |
| Business error | Frontend must check `code`, not only HTTP status |
| Auth header | `Authorization: Bearer <token>` |
| Token expiry | `data.expires_in`, currently 3600 seconds |

All frontend API calls should go through `frontend/src/api/client.ts`.

## Auth Pages

| Page | Action | Method And Path | Request | Response `data` |
|------|--------|-----------------|---------|-----------------|
| 登录 | 登录 | `POST /v1/auth/login` | `{ "username": "13800000000", "password": "123456" }` | `{ "token": "...", "expires_in": 3600 }` |
| 注册 | 获取验证码 | `POST /v1/auth/send-verification-code` | `{ "phone": "13800000000" }` | `{ "expires_in": 300, "test_code": "123456" }` |
| 注册 | 注册账号 | `POST /v1/auth/register` | `{ "phone": "...", "verificationCode": "...", "password": "..." }` | `null` |
| 忘记密码 | 重置密码 | `POST /v1/auth/forgot-password` | `{ "phone": "...", "verificationCode": "...", "newPassword": "..." }` | `null` |
| 个人资料 | 获取资料 | `GET /v1/auth/profile` | none | `Profile` |
| 个人资料 | 更新资料 | `PUT /v1/auth/profile` | `UpdateProfileRequest` | `Profile` |
| 个人资料 | 修改密码 | `PUT /v1/auth/password` | `{ "oldPassword": "...", "newPassword": "..." }` | `null` |
| 个人资料 | 登出 | `POST /v1/auth/logout` | none | `null` |

`Profile` is used for display and menu permission checks. `profile.role` controls admin routes.

## Dashboard

| Block | Method And Path | Auth | Purpose |
|-------|-----------------|------|---------|
| Profile | `GET /v1/auth/profile` | Yes | 恢复登录态和用户菜单 |
| Nodes | `GET /v1/nodes` | Yes | 首页节点卡片和默认节点 |
| Health | `GET /v1/health` | No | 展示后端可用性和课程版标识 |
| Alert summary | `POST /v1/alerts/list` | No | 未处理告警数量 |

Recommended initialization:

1. Restore token and profile.
2. Load authorized nodes.
3. Load health and alert summary for `SIM_Node_01`.

## Node Pages

| Page / Block | Method And Path | Request | Response Focus |
|--------------|-----------------|---------|----------------|
| 节点详情 | `GET /v1/nodes/{nodeId}` | path `nodeId` | `id`, `name`, `status`, `nodeState` |
| 传感器元数据 | `GET /v1/nodes/{nodeId}/sensor-meta` | path `nodeId` | `key`, `name`, `unit`, thresholds |
| 设备元数据 | `GET /v1/nodes/{nodeId}/equipment-meta` | path `nodeId` | `key`, `name`, `controllable`, `controlType` |
| 阈值更新 | `PUT /v1/nodes/{nodeId}/sensor-thresholds` | `ThresholdUpdate[]` | `null` |

Threshold request:

```json
[
  {
    "sensorKey": "temperature",
    "warningMin": 15.0,
    "warningMax": 35.0
  }
]
```

## Sensor History

| Action | Method And Path | Query | Response Focus |
|--------|-----------------|-------|----------------|
| 查询历史 | `GET /v1/sensors/{nodeId}/history` | `metric`, `range` | `metric`, `summary`, `points` |

Supported `metric`: `temperature`, `humidity`, `light`, `soilHumidity`.

Supported `range`: `24h`, `7d`, `30d`.

Course simulation history is rolling and stable: the first request initializes the point list, and each later request appends only the newest point while preserving the previous points already shown by the chart.

Response fields:

| Field | Type | Frontend Usage |
|-------|------|----------------|
| `summary.max` | number | 最大值摘要 |
| `summary.min` | number | 最小值摘要 |
| `summary.avg` | number | 平均值摘要 |
| `points[].timeLabel` | string | 图表横轴 |
| `points[].value` | number | 图表数值 |

## Equipment Stats

| Action | Method And Path | Request | Response Focus |
|--------|-----------------|---------|----------------|
| 查询统计 | `POST /v1/equipment/stats` | `{ "node_id": "SIM_Node_01", "equipment_key": "water_pump" }` | `summary`, `points` |

Supported equipment keys: `water_pump`, `led_light`, `fan`.

Frontend should always send `node_id`. If it is omitted, the backend falls back to the course node `SIM_Node_01`. Frontend only displays stats and demo controls; it does not send real device commands.

## Alerts

| Action | Method And Path | Request | Response Focus |
|--------|-----------------|---------|----------------|
| 查询告警 | `POST /v1/alerts/list` | `{ "query": { "node_id": "SIM_Node_01", "filter_status": "unhandled", "page": 1, "pageSize": 10 } }` | `total`, scoped `unhandled_count`, `items` |
| 处理告警 | `POST /v1/alerts/handle` | `{ "data": { "alert_id": "COURSE_ALERT_1", "action": "handle" } }` | `null` |

`filter_status` values:

| Value | Meaning |
|-------|---------|
| `all` | 全部 |
| `unhandled` | 未处理 |
| `handled` | 已处理 |

Alert response fields use `snake_case`: `alert_id`, `node_id`, `type_name`, `source_name`, `is_handled`, `unhandled_count`, `handled_recent_count`. The frontend accepts both snake_case and camelCase alert fields defensively, but new backend responses should keep snake_case.

Realtime dashboard surfaces refresh their sensor and alert summaries every 2 seconds. During demos, the simulated 24h air-temperature stream periodically drops below `5C`; the backend then creates a low-temperature warning after the refresh loop observes that value.

## Scheduled Tasks

| Action | Method And Path | Request | Response Focus |
|--------|-----------------|---------|----------------|
| 列表 | `GET /v1/tasks` | none | `TaskInfo[]` |
| 创建 | `POST /v1/tasks` | `TaskRequest` | `TaskInfo` |
| 更新 | `PUT /v1/tasks/{taskId}` | `TaskRequest` | `TaskInfo` |
| 删除 | `DELETE /v1/tasks/{taskId}` | none | `null` |
| 启停 | `PUT /v1/tasks/{taskId}/toggle` | none | `null` |

`TaskRequest`:

```json
{
  "nodeId": "SIM_Node_01",
  "equipmentKey": "water_pump",
  "action": "turn_on",
  "taskName": "每日灌溉",
  "hour": 8,
  "minute": 30,
  "repeatDays": "1,2,3,4,5,6,7"
}
```

Form rules:

| Field | Rule |
|-------|------|
| `nodeId` | 必填，默认 `SIM_Node_01` |
| `equipmentKey` | 必须来自设备元数据 |
| `action` | 必填，建议 `turn_on` / `turn_off` |
| `taskName` | 必填，建议 2 到 30 个字符 |
| `hour` | 0 到 23 |
| `minute` | 0 到 59 |
| `repeatDays` | 逗号分隔的 `1` 到 `7` |

## Admin Pages

| Block | Method And Path | Purpose |
|-------|-----------------|---------|
| 节点管理 | `POST /v1/admin/nodes` | 创建节点 |
| 节点管理 | `PUT /v1/admin/nodes/{nodeId}` | 更新节点名称 |
| 节点管理 | `DELETE /v1/admin/nodes/{nodeId}` | 删除节点 |
| 用户管理 | `GET /v1/admin/users` | 用户列表 |
| 用户管理 | `PUT /v1/admin/users/{userId}/role` | 更新角色 |
| 授权管理 | `GET /v1/admin/users/{userId}/nodes` | 查询授权节点 |
| 授权管理 | `PUT /v1/admin/users/{userId}/nodes` | 设置授权节点 |
| 操作日志 | `GET /v1/admin/logs?page=1&size=20&type=LOGIN` | 查询审计日志 |

## Error Mapping

| `code` | Frontend Handling |
|--------|-------------------|
| `200` | 使用 `data` 渲染页面 |
| `400` | 展示表单或请求参数错误 |
| `401` | 清理登录态并跳转登录页 |
| `403` | 展示无权限状态 |
| `404` | 展示资源不存在或空状态 |
| `409` | 展示冲突原因 |
| Other | 展示通用错误并提供重试 |
