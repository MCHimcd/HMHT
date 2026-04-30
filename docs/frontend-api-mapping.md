# HMHT 课程版前端 API 映射

本文把首版前端页面映射到后端 REST API。详细接口定义以 `docs/api-endpoints.md` 为准，本文重点服务前端开发和联调。

## 通用约定

| 项 | 值 |
|----|----|
| 后端默认地址 | `http://localhost:8080` |
| API 基路径 | `/api` |
| 前端请求基址 | `http://localhost:8080/api` 或开发代理后的 `/api` |
| 成功格式 | `{ "code": 200, "message": "success", "data": ... }` |
| 业务错误 | HTTP 可能仍为 200，前端必须检查 `code` |
| 认证头 | `Authorization: Bearer <token>` |
| token 有效期 | 登录响应 `data.expires_in`，当前为 3600 秒 |

前端请求封装必须先解析统一响应，再把 `data` 交给页面使用；当 `code != 200` 时进入业务错误流程。

## 认证页面

| 页面 | 操作 | 方法与路径 | 请求 | 返回 `data` | 备注 |
|------|------|------------|------|-------------|------|
| 登录 | 登录 | `POST /v1/auth/login` | `{ "username": "13800000000", "password": "123456" }` | `{ "token": "...", "expires_in": 3600 }` | 成功后保存 token |
| 注册 | 获取验证码 | `POST /v1/auth/send-verification-code` | `{ "phone": "13800000000" }` | `{ "expires_in": 300, "test_code": "123456" }` | 课程版返回测试验证码 |
| 注册 | 注册账号 | `POST /v1/auth/register` | `{ "phone": "...", "verificationCode": "...", "password": "..." }` | `null` | 成功后返回登录页 |
| 忘记密码 | 重置密码 | `POST /v1/auth/forgot-password` | `{ "phone": "...", "verificationCode": "...", "newPassword": "..." }` | `null` | 可选首版页面 |
| 个人资料 | 获取资料 | `GET /v1/auth/profile` | 无 | `Profile` | 需要 token |
| 个人资料 | 更新资料 | `PUT /v1/auth/profile` | `UpdateProfileRequest` | `Profile` | 需要 token |
| 个人资料 | 修改密码 | `PUT /v1/auth/password` | `{ "oldPassword": "...", "newPassword": "..." }` | `null` | 需要 token |
| 个人资料 | 登出 | `POST /v1/auth/logout` | 无 | `null` | 成功或失败都清理前端登录态 |

`Profile` 字段：

| 字段 | 类型 | 用途 |
|------|------|------|
| `userId` | string | 用户标识 |
| `userName` | string | 显示名称 |
| `phone` | string | 手机号 |
| `gender` | string | 性别，可为空 |
| `birthDate` | string | 出生日期，可为空 |
| `avatarSrc` | string | 头像地址，可为空；课程版不做上传 |
| `role` | string | 角色，管理员入口显示依据 |

## 首页概览

| 页面区块 | 方法与路径 | 认证 | 前端用途 |
|----------|------------|------|----------|
| 节点列表 | `GET /v1/nodes` | 是 | 首页节点卡片、默认进入第一个节点 |
| 健康状态 | `GET /v1/health` | 否 | 展示后端可用性和课程版标识 |
| 告警摘要 | `POST /v1/alerts/list` | 否 | 请求未处理告警数量 |

建议首页初始化顺序：

1. 读取 `GET /v1/auth/profile` 恢复当前用户。
2. 读取 `GET /v1/nodes` 获取授权节点。
3. 对默认节点 `SIM_Node_01` 读取告警摘要和健康状态。

## 节点详情

| 页面区块 | 方法与路径 | 请求 | 返回重点 |
|----------|------------|------|----------|
| 节点详情 | `GET /v1/nodes/{nodeId}` | path: `nodeId` | `id`、`name`、`status`、`nodeState` |
| 传感器元数据 | `GET /v1/nodes/{nodeId}/sensor-meta` | path: `nodeId` | `key`、`name`、`unit`、`warningMin`、`warningMax` |
| 设备元数据 | `GET /v1/nodes/{nodeId}/equipment-meta` | path: `nodeId` | `key`、`name`、`controllable`、`controlType` |
| 阈值更新 | `PUT /v1/nodes/{nodeId}/sensor-thresholds` | `ThresholdUpdate[]` | `null` |

`ThresholdUpdate`：

```json
[
  {
    "sensorKey": "temperature",
    "warningMin": 15.0,
    "warningMax": 35.0
  }
]
```

## 传感器曲线

| 操作 | 方法与路径 | 查询参数 | 返回重点 |
|------|------------|----------|----------|
| 查询历史 | `GET /v1/sensors/{nodeId}/history` | `metric`、`range` | `metric`、`summary`、`points` |

支持的 `metric`：

| 值 | 名称 | 单位 |
|----|------|------|
| `temperature` | 空气温度 | `C` |
| `humidity` | 空气湿度 | `%` |
| `light` | 光照强度 | `Lux` |
| `soilHumidity` | 土壤湿度 | `%` |

支持的 `range`：

| 值 | 说明 |
|----|------|
| `24h` | 最近 24 小时 |
| `7d` | 最近 7 天 |
| `30d` | 最近 30 天 |

响应字段：

| 字段 | 类型 | 前端用途 |
|------|------|----------|
| `summary.max` | number | 最大值摘要 |
| `summary.min` | number | 最小值摘要 |
| `summary.avg` | number | 平均值摘要 |
| `points[].timeLabel` | string | 图表横轴 |
| `points[].value` | number | 图表数值 |

## 设备统计

| 操作 | 方法与路径 | 请求 | 返回重点 |
|------|------------|------|----------|
| 查询统计 | `POST /v1/equipment/stats` | `{ "node_id": "SIM_Node_01", "equipment_key": "water_pump" }` | `summary`、`points` |

支持的 `equipmentKey`：

| 值 | 名称 | 控制类型 |
|----|------|----------|
| `water_pump` | 水泵 | `toggle` |
| `led_light` | 补光灯 | `slider` |
| `fan` | 排风扇 | `toggle` |

前端首版只展示统计，不实现真实设备控制。

## 告警列表

| 操作 | 方法与路径 | 请求 | 返回重点 |
|------|------------|------|----------|
| 查询告警 | `POST /v1/alerts/list` | `{ "query": { "nodeId": "SIM_Node_01", "handled": false, "page": 1, "pageSize": 10 } }` | `total`、`unhandled_count`、`items` |
| 处理告警 | `POST /v1/alerts/handle` | `{ "data": { "alert_id": "COURSE_ALERT_1", "action": "handle" } }` | `null` |

告警返回字段使用 snake_case：

| 字段 | 类型 | 前端用途 |
|------|------|----------|
| `alert_id` | string | 告警标识 |
| `node_id` | string | 节点标识 |
| `type` | string | 告警类型 |
| `type_name` | string | 展示名称 |
| `level` | string | 严重程度样式 |
| `message` | string | 告警文案 |
| `source_name` | string | 来源展示 |
| `is_handled` | boolean | 是否已处理 |
| `timestamp` | number | 告警时间 |
| `handle_time` | number | 处理时间，可为空 |

## 定时任务

| 操作 | 方法与路径 | 请求 | 返回重点 |
|------|------------|------|----------|
| 任务列表 | `GET /v1/tasks` | 无 | `TaskInfo[]` |
| 创建任务 | `POST /v1/tasks` | `TaskRequest` | `TaskInfo` |
| 更新任务 | `PUT /v1/tasks/{taskId}` | `TaskRequest` | `TaskInfo` |
| 删除任务 | `DELETE /v1/tasks/{taskId}` | 无 | `null` |
| 启停任务 | `PUT /v1/tasks/{taskId}/toggle` | 无 | `null` |

`TaskRequest`：

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

前端表单约束：

| 字段 | 规则 |
|------|------|
| `nodeId` | 必填，首版默认 `SIM_Node_01` |
| `equipmentKey` | 必须来自设备元数据 |
| `action` | 必填，建议首版提供 `turn_on`、`turn_off` |
| `taskName` | 必填，建议 2 到 30 个字符 |
| `hour` | 0 到 23 |
| `minute` | 0 到 59 |
| `repeatDays` | 逗号分隔的 `1` 到 `7` |

## 管理页面映射

管理员功能首版可延后；如实现，建议独立放在 `/admin`。

| 页面区块 | 方法与路径 | 用途 |
|----------|------------|------|
| 节点管理 | `POST /v1/admin/nodes` | 创建节点 |
| 节点管理 | `PUT /v1/admin/nodes/{nodeId}` | 更新节点名称 |
| 节点管理 | `DELETE /v1/admin/nodes/{nodeId}` | 删除节点 |
| 用户管理 | `GET /v1/admin/users` | 用户列表 |
| 用户管理 | `PUT /v1/admin/users/{userId}/role` | 更新角色 |
| 授权管理 | `GET /v1/admin/users/{userId}/nodes` | 查询授权节点 |
| 授权管理 | `PUT /v1/admin/users/{userId}/nodes` | 设置授权节点 |
| 操作日志 | `GET /v1/admin/logs?page=1&size=20&type=LOGIN` | 查询审计日志 |

## 前端错误处理映射

| `code` | 处理 |
|--------|------|
| `200` | 使用 `data` 渲染页面 |
| `400` | 表单或请求参数错误，停留当前页面并展示错误 |
| `401` | token 无效或过期，清理登录态并跳转登录页 |
| `403` | 无权限，展示无权限状态 |
| `404` | 资源不存在，展示空状态或返回上级页面 |
| `409` | 冲突，提示手机号、节点或业务数据已存在 |
| 其他 | 展示通用错误并提供重试 |
