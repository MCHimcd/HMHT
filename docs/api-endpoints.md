# HMHT Course API

Base path: `/api`.

All endpoints return the unified response shape:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Business errors are represented by `code` and `message`; the HTTP status may still be `200`.

## Auth

Prefix: `/v1/auth`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/v1/auth/send-verification-code` | 获取测试验证码 | No |
| POST | `/v1/auth/register` | 注册用户 | No |
| POST | `/v1/auth/login` | 登录并返回业务 token | No |
| POST | `/v1/auth/forgot-password` | 重置密码 | No |
| GET | `/v1/auth/profile` | 获取个人资料 | Bearer Token |
| PUT | `/v1/auth/profile` | 更新个人资料 | Bearer Token |
| PUT | `/v1/auth/password` | 修改密码 | Bearer Token |
| POST | `/v1/auth/logout` | 登出 | Bearer Token |

Login request:

```json
{
  "username": "13800000000",
  "password": "123456"
}
```

Login response `data`:

```json
{
  "token": "uuid-token",
  "expires_in": 3600
}
```

## Nodes

Prefix: `/v1/nodes`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/v1/nodes` | 获取当前用户可见节点 | Bearer Token |
| GET | `/v1/nodes/{nodeId}` | 获取节点详情 | Bearer Token |
| GET | `/v1/nodes/{nodeId}/sensor-meta` | 获取固定传感器元数据 | No |
| GET | `/v1/nodes/{nodeId}/equipment-meta` | 获取固定设备元数据 | No |
| PUT | `/v1/nodes/{nodeId}/sensor-thresholds` | 更新传感器阈值 | Bearer Token |

Course seed node: `SIM_Node_01`.

Sensor types:

| Key | Name | Unit |
|-----|------|------|
| `temperature` | 空气温度 | `C` |
| `humidity` | 空气湿度 | `%` |
| `light` | 光照强度 | `Lux` |
| `soilHumidity` | 土壤湿度 | `%` |

Equipment types:

| Key | Name | Control Type |
|-----|------|--------------|
| `water_pump` | 水泵 | `toggle` |
| `led_light` | 补光灯 | `slider` |
| `fan` | 排风扇 | `toggle` |

Threshold update request:

```json
[
  {
    "sensorKey": "temperature",
    "warningMin": 15.0,
    "warningMax": 35.0
  }
]
```

## Sensor And Equipment

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/v1/sensors/{nodeId}/history?metric=temperature&range=24h` | 生成模拟传感器曲线 | No |
| POST | `/v1/equipment/stats` | 生成模拟设备统计 | No |

Supported `range`:

| Range | Description |
|-------|-------------|
| `24h` | 最近 24 小时 |
| `7d` | 最近 7 天 |
| `30d` | 最近 30 天 |

Equipment stats request:

```json
{
  "node_id": "SIM_Node_01",
  "equipment_key": "water_pump"
}
```

`/v1/equipment/stats` also accepts wrapper fields such as `query.node_id`, `device_id`, `query.equipment_key`, `equipment_type`, and `device_type`. New frontend code should use `node_id` and `equipment_key`. If the node id is omitted, the backend uses the course node `SIM_Node_01`.

## Alerts

Prefix: `/v1/alerts`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/v1/alerts/list` | 查询告警列表 | No |
| POST | `/v1/alerts/handle` | 处理告警 | No |

List request:

```json
{
  "query": {
    "node_id": "SIM_Node_01",
    "filter_status": "unhandled",
    "page": 1,
    "pageSize": 10
  }
}
```

`filter_status` supports `all`, `handled`, and `unhandled`. The controller also normalizes `query.size` to `query.pageSize`.

Handle request:

```json
{
  "data": {
    "alert_id": "COURSE_ALERT_1",
    "action": "handle"
  }
}
```

Alert list response fields use `snake_case`, including `unhandled_count`, `handled_recent_count`, `alert_id`, `node_id`, `type_name`, `source_name`, `is_handled`, and `handle_time`.

## Scheduled Tasks

Prefix: `/v1/tasks`.

Tasks store configuration only; they do not send commands to real devices.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/v1/tasks` | 列出当前用户任务 | Bearer Token |
| POST | `/v1/tasks` | 创建任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}` | 更新任务 | Bearer Token |
| DELETE | `/v1/tasks/{taskId}` | 删除任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}/toggle` | 切换启用状态 | Bearer Token |

Create/update request:

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

## Admin

Prefix: `/v1/admin`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/v1/admin/nodes` | 创建节点 | Bearer Token, admin |
| PUT | `/v1/admin/nodes/{nodeId}` | 更新节点 | Bearer Token, admin |
| DELETE | `/v1/admin/nodes/{nodeId}` | 删除节点 | Bearer Token, admin |
| GET | `/v1/admin/users` | 列出用户 | Bearer Token, admin |
| PUT | `/v1/admin/users/{userId}/role` | 更新用户角色 | Bearer Token, admin |
| GET | `/v1/admin/users/{userId}/nodes` | 查询用户授权节点 | Bearer Token, admin |
| PUT | `/v1/admin/users/{userId}/nodes` | 设置用户授权节点 | Bearer Token, admin |

The course seed data creates `SIM_Node_01`, but admin APIs can still create extra demo nodes.

## Operation Logs

Prefix: `/v1/admin/logs`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/v1/admin/logs?page=1&size=20&type=LOGIN` | 查询操作日志 | No |

## Health

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/v1/health` | 查询课程版服务状态 | No |

Response `data`:

```json
{
  "available": true,
  "application": "HMHT",
  "edition": "course",
  "nodeId": "SIM_Node_01",
  "dataSimulated": true,
  "time": "2026-04-23T12:00:00+08:00"
}
```
