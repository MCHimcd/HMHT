# HMHT 课程版 API 文档

基路径：`/api`

统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

业务错误仍返回 HTTP 200，由 `code` 表示业务状态码。

## 认证 `/v1/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/auth/send-verification-code` | 获取测试验证码 | 否 |
| POST | `/v1/auth/register` | 注册用户 | 否 |
| POST | `/v1/auth/login` | 登录并返回内存 token | 否 |
| POST | `/v1/auth/forgot-password` | 重置密码 | 否 |
| GET | `/v1/auth/profile` | 获取个人资料 | Bearer Token |
| PUT | `/v1/auth/profile` | 更新个人资料 | Bearer Token |
| PUT | `/v1/auth/password` | 修改密码 | Bearer Token |
| POST | `/v1/auth/logout` | 登出 | Bearer Token |

### `POST /v1/auth/login`

请求：

```json
{
  "username": "13800000000",
  "password": "123456"
}
```

响应 `data`：

```json
{
  "token": "uuid-token",
  "expires_in": 3600
}
```

## 节点 `/v1/nodes`

课程版只保留一个模拟节点：`SIM_Node_01`。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/nodes` | 获取当前用户可见节点 | Bearer Token |
| GET | `/v1/nodes/{nodeId}` | 获取节点详情 | Bearer Token |
| GET | `/v1/nodes/{nodeId}/sensor-meta` | 获取固定传感器元数据 | 否 |
| GET | `/v1/nodes/{nodeId}/equipment-meta` | 获取固定设备元数据 | 否 |
| PUT | `/v1/nodes/{nodeId}/sensor-thresholds` | 更新传感器阈值 | Bearer Token |

固定传感器类型：

| key | name | unit |
|-----|------|------|
| `temperature` | 空气温度 | `C` |
| `humidity` | 空气湿度 | `%` |
| `light` | 光照强度 | `Lux` |
| `soilHumidity` | 土壤湿度 | `%` |

固定设备类型：

| key | name | controlType |
|-----|------|-------------|
| `water_pump` | 水泵 | `toggle` |
| `led_light` | 补光灯 | `slider` |
| `fan` | 排风扇 | `toggle` |

### `PUT /v1/nodes/{nodeId}/sensor-thresholds`

请求：

```json
[
  {
    "sensorKey": "temperature",
    "warningMin": 15.0,
    "warningMax": 35.0
  }
]
```

## 传感器与设备

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/sensors/{nodeId}/history?metric=temperature&range=24h` | 生成模拟传感器曲线 | 否 |
| POST | `/v1/equipment/stats` | 生成模拟设备统计 | 否 |

支持的 `range`：

| range | 说明 |
|-------|------|
| `24h` | 最近 24 小时 |
| `7d` | 最近 7 天 |
| `30d` | 最近 30 天 |

### `POST /v1/equipment/stats`

请求：

```json
{
  "nodeId": "SIM_Node_01",
  "equipmentKey": "water_pump"
}
```

## 告警 `/v1/alerts`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/alerts/list` | 查询告警列表 | 否 |
| POST | `/v1/alerts/handle` | 处理告警 | 否 |

### `POST /v1/alerts/list`

请求：

```json
{
  "nodeId": "SIM_Node_01",
  "handled": false,
  "page": 1,
  "size": 10
}
```

### `POST /v1/alerts/handle`

请求：

```json
{
  "alertId": "COURSE_ALERT_1",
  "action": "handle"
}
```

## 定时任务 `/v1/tasks`

课程版任务只保存配置，不向真实设备下发命令。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/tasks` | 列出当前用户任务 | Bearer Token |
| POST | `/v1/tasks` | 创建任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}` | 更新任务 | Bearer Token |
| DELETE | `/v1/tasks/{taskId}` | 删除任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}/toggle` | 切换任务启用状态 | Bearer Token |

### `POST /v1/tasks`

请求：

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

## 管理 `/v1/admin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/admin/nodes` | 创建节点 | Bearer Token, admin |
| PUT | `/v1/admin/nodes/{nodeId}` | 更新节点 | Bearer Token, admin |
| DELETE | `/v1/admin/nodes/{nodeId}` | 删除节点 | Bearer Token, admin |
| GET | `/v1/admin/users` | 列出用户 | Bearer Token, admin |
| PUT | `/v1/admin/users/{userId}/role` | 更新用户角色 | Bearer Token, admin |
| GET | `/v1/admin/users/{userId}/nodes` | 查询用户授权节点 | Bearer Token, admin |
| PUT | `/v1/admin/users/{userId}/nodes` | 设置用户授权节点 | Bearer Token, admin |

虽然课程版默认只种子化 `SIM_Node_01`，管理接口仍可用于课程 CRUD 演示。

## 操作日志 `/v1/admin/logs`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/admin/logs?page=1&size=20&type=LOGIN` | 查询操作日志 | 否 |

## 健康检查 `/v1/health`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/health` | 查询课程版服务状态 | 否 |

响应 `data`：

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
