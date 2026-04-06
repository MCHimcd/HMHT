# 硬件成员执行指南（HMHT V2 对接）

本文档给硬件团队明确“需要做什么”，用于完成 HMHT V2 协议接入。

## 1. 先完成的基础项

1. 为每台设备分配唯一 `device_id`，并确保设备重启后不变化。
2. 固件支持 MQTT QoS=1，且可配置 broker 地址、账号、密码。
3. 时间同步到毫秒级（NTP 或网关授时），保证 `timestamp` 可用于时序分析。
4. 每条消息生成唯一 `trace_id`（UUID 推荐）。

## 2. 必须实现的 Topic

1. 发布遥测：`/hmht/device/{deviceId}/telemetry`
2. 订阅命令：`/hmht/device/{deviceId}/command`
3. 发布回执：`/hmht/device/{deviceId}/ack`

推荐实现：

1. 发布能力：`/hmht/device/{deviceId}/capability`
2. 发布心跳：`/hmht/device/{deviceId}/heartbeat`

## 3. 消息信封必须一致

所有消息必须包含以下字段：

- `msg_type`
- `protocol_version`（建议固定 `2.0`）
- `device_id`
- `timestamp`（epoch ms）
- `trace_id`
- `payload`

注意：字段缺失会被后端判定为死信。

## 4. 遥测上报要求

1. `payload.sensors` 与 `payload.equipments` 都允许动态 key，硬件可按能力实际数量上报。
2. 传感器值建议使用结构：`{"value": 12.3, "unit": "C"}`。
3. 设备状态建议使用结构：`{"state": "on"}`。
4. 新增 key 不需要等待后端改代码，但需要在能力上报中声明。

## 5. 命令执行与 ACK 要求

1. 接收到命令后按 `payload.command_id` 原样回传 ACK。
2. ACK 至少包含：`command_id`、`status`、`ack_code`、`ack_message`、`acked_at`。
3. `status` 使用：`accepted` / `executed` / `rejected` / `failed`。
4. 建议 ACK 时限：收到命令后 5 秒内返回第一条 ACK。

## 6. ACK 错误码约定

1. `0`：成功
2. `1001`：不支持的 target_key
3. `1002`：不支持的 action
4. `1003`：参数非法
5. `2001`：设备忙
6. `2002`：执行超时
7. `3001`：设备内部错误

## 7. 能力上报建议（强烈推荐）

设备启动后与能力变更时上报：

1. `sensors`：每个 sensor 的 key、数据类型、单位、范围。
2. `equipments`：每个设备 key、可执行动作列表、是否可控。
3. `firmware`：版本、厂商、型号。

这样 App 才能动态渲染，不依赖写死字段。

## 8. 联调检查清单

1. 单设备 10 分钟连续上报 telemetry，不丢包。
2. 连续下发 20 条 command，ACK 成功率 >= 99%。
3. 故意发送 3 条非法命令，ACK 错误码正确。
4. 新增 1 个自定义 sensor_key，后端可接收并可查询。
5. 断网重连后，设备可恢复上报且 `device_id` 不变。

## 9. 交付给后端的最小信息

1. 设备型号与固件版本。
2. 每类设备的 `target_key` 与 `action` 白名单。
3. 每个传感器 key 的单位、值域、采样间隔。
4. ACK 超时策略与重试策略说明。

## 10. 常见问题

1. 命令收到但无 ACK：后端会记录超时并触发重试。
2. 新 sensor 无法展示：先确认 capability 是否上报该 key。
3. 时间错乱：请检查设备时钟，确保 `timestamp` 使用毫秒。

参考协议：`docs/backend-hardware-protocol.md`
