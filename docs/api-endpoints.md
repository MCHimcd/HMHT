# REST API 端点清单

基路径：`/api`（`server.servlet.context-path=/api`）

## 认证 — AuthController (`/v1/auth`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/auth/login` | 用户登录 | 否 |
| POST | `/v1/auth/send-verification-code` | 发送验证码 | 否 |
| POST | `/v1/auth/register` | 用户注册 | 否 |
| POST | `/v1/auth/forgot-password` | 忘记密码 | 否 |
| GET | `/v1/auth/profile` | 获取个人资料 | Bearer Token |
| PUT | `/v1/auth/profile` | 更新个人资料 | Bearer Token |
| PUT | `/v1/auth/password` | 修改密码 | Bearer Token |
| POST | `/v1/auth/avatar/use-fixed` | 使用固定头像 | Bearer Token |
| POST | `/v1/auth/avatar/upload` | 上传头像（multipart/form-data） | Bearer Token |
| POST | `/v1/auth/logout` | 登出 | Bearer Token |

### 头像上传接口详情

**请求格式**: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | binary | 是 | 头像图片文件，支持 PNG/JPEG，最大 5MB |

存储目录由 `hmht.upload.avatar-dir`（默认 `uploads/avatars`）控制；后端会解析为绝对路径后落盘，避免 Servlet 临时目录相对路径导致的写入失败。

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "WangNong_001",
    "userName": "王农",
    "gender": "男",
    "birthDate": "1990-01-01",
    "avatarSrc": "http://localhost:8080/api/uploads/avatars/user_1_20260407.png",
    "role": "user"
  }
}
```

**错误响应**:
- `400`: 文件为空 / 仅支持 PNG/JPEG 格式图片 / 文件大小不能超过 5MB
- `401`: 未登录或 token 已过期

## 节点管理 — NodeController (`/v1/nodes`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/nodes` | 获取用户节点列表 | Bearer Token |
| GET | `/v1/nodes/{nodeId}` | 获取节点详情 | Bearer Token |
| GET | `/v1/nodes/{nodeId}/sensor-meta` | 获取传感器元数据 | 否 |
| GET | `/v1/nodes/{nodeId}/equipment-meta` | 获取设备元数据 | 否 |
| PUT | `/v1/nodes/{nodeId}/sensor-thresholds` | 更新传感器阈值 | Bearer Token |

**节点在线状态与元数据来源**：

- 在线状态：优先读取 `device_profile`（`online + last_seen_at`，心跳窗口 60 秒），无设备档案时回退 `nodes.online`。
- 传感器/设备元数据：若存在最新 capability 快照，优先使用 `capability_sensor` / `capability_equipment`；若 capability 不存在再回退 `node_sensor` / `node_equipment` 绑定。
- 未在 `sensor_type` / `equipment_type` 字典中定义的动态 key 会保留并返回通用元数据，避免前端遗漏硬件真实能力。

## 设备能力 — DeviceController (`/v1/devices`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/devices/{deviceId}/capability` | 获取设备能力快照（sensor/equipment/action） | 否 |
| GET | `/v1/devices/{deviceId}/shadow` | 获取设备影子快照（最近 ACK 状态） | 否 |

## 传感器 & 设备 — SensorController

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/sensors/{nodeId}/history` | 传感器历史数据（?metric=&range=） | 否 |
| POST | `/v1/equipment/stats` | 设备统计数据 | 否 |

## 告警管理 — AlertController (`/v1/alerts`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/alerts/list` | 查询告警列表 | 否 |
| POST | `/v1/alerts/handle` | 处理告警 | 否 |

## 定时任务 — ScheduledTaskController (`/v1/tasks`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/tasks` | 列出当前用户任务 | Bearer Token |
| POST | `/v1/tasks` | 创建任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}` | 更新任务 | Bearer Token |
| DELETE | `/v1/tasks/{taskId}` | 删除任务 | Bearer Token |
| PUT | `/v1/tasks/{taskId}/toggle` | 切换任务启用状态 | Bearer Token |

## AI 对话 — ChatController (`/v1/agri-qa`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/agri-qa/chat` | 发送消息（支持图像） | 否 |

## 溯源 & 摄像头 — ExtraController

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/traceability/scan` | 扫描溯源码 | 否 |
| GET | `/v1/cameras/{nodeId}/live` | 获取直播流 | 否 |
| GET | `/v1/cameras/{nodeId}/playback` | 获取回放列表 | 否 |
| GET | `/v1/cameras/{nodeId}/keyframes` | 获取关键帧列表 | 否 |

## 管理 — AdminController (`/v1/admin`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/v1/admin/nodes` | 创建节点 | Bearer Token |
| PUT | `/v1/admin/nodes/{nodeId}` | 更新节点 | Bearer Token |
| DELETE | `/v1/admin/nodes/{nodeId}` | 删除节点 | Bearer Token |
| GET | `/v1/admin/users` | 列出所有用户 | Bearer Token |
| PUT | `/v1/admin/users/{userId}/role` | 更新用户角色 | Bearer Token |
| GET | `/v1/admin/users/{userId}/nodes` | 获取用户节点列表 | Bearer Token |
| PUT | `/v1/admin/users/{userId}/nodes` | 设置用户节点权限 | Bearer Token |

## 操作日志 — OperationLogController (`/v1/admin/logs`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/admin/logs` | 查询操作日志（?page=&size=&type=） | 否 |

## 健康检查 — HealthController (`/v1`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/v1/health` | 前端可用性检查（统一 ApiResponse） | 否 |

## 基础设施 — InfraController (`/infra`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/infra/health` | 健康检查（含 MQTT 状态） | 否 |
| POST | `/infra/mqtt/publish` | 手动发布 MQTT 消息 | 否 |
