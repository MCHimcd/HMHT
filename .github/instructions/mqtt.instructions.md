---
description: "MQTT 桥接层约定 - 主题契约、条件装配、遥测映射、修改须知"
applyTo: "src/main/java/ww/wangnong/mqtt/**/*.java,src/main/java/ww/wangnong/bridge/**/*.java"
---

# MQTT 桥接层约定

## 条件装配

桥接相关组件按运行模式激活（条件注解详见 `java.instructions.md`）：

| 组件 | 条件注解 |
|------|----------|
| `MqttBridge` | `@ConditionalOnBridgeMode` |
| `ProtocolPersistenceService` | `@ConditionalOnBridgeMode` |
| `CommandAckTracker` | `@ConditionalOnHardwareRoute` |
| `AgriChatService` | `@ConditionalOnPythonRoute` |
| `MockChatService` | `@ConditionalOnNoPythonRoute` |

## 主题契约

### HMHT 订阅的主题

| 主题 | 来源 | 处理 |
|------|------|------|
| `/agri/device/+/telemetry` | Python 后端 | 翻译 → App + InfluxDB |
| `/hmht/device/+/telemetry` | 硬件设备 | V2 规范翻译 → App + InfluxDB |
| `/hmht/device/+/capability` | 硬件设备 | 持久化能力快照 |
| `/hmht/device/+/ack` | 硬件设备 | 透传 App + 更新 shadow |
| `/agri/ai_vision/alert` | Python AI | 创建告警 → App |
| `/wangnong_2026/node/+/command` | App 端 | 双路转发（agri + hmht） |

### HMHT 发布的主题

| 主题 | 目标 | 内容 |
|------|------|------|
| `/wangnong_2026/node/{nodeId}/telemetry` | App | 翻译后遥测 |
| `/wangnong_2026/node/{nodeId}/alert` | App | 告警 JSON |
| `/agri/device/{deviceId}/command` | Python | 控制命令 |
| `/hmht/device/{deviceId}/command` | 硬件 | V2 控制命令 |
| `/wangnong_2026/node/{nodeId}/ack` | App | 硬件回执 |

## 遥测字段映射（TelemetryTranslator）

`pyToApp()` 将 Python 字段名翻译为 App 端字段名：

| Python 字段 | App 字段 |
|-------------|----------|
| `air_temp` | `temperature` |
| `air_humidity` | `humidity` |
| `soil_temp` | `soilTemp` |
| `soil_moisture` | `soilHumidity` |
| `soil_ph` | `soilPh` |
| `light_intensity` | `light` |
| `co2_ppm` | `co2` |
| `soil_n` | `soilN` |
| `soil_p` | `soilP` |
| `soil_k` | `soilK` |

`hardwareV2ToApp()` 规范化硬件 V2 遥测（动态 key 透传）。

## PahoMqttService 规则

- **所有 MQTT 操作通过 PahoMqttService**，不直接创建 Paho 客户端实例
- 订阅回调线程安全
- 支持通配符主题匹配（`+` 单层，`#` 多层）

## ACK 重试闭环

- 命令下发后写入 `command_request`
- ACK 到达写入 `command_ack` 并更新主状态
- `CommandAckTracker` 定时扫描超时命令（默认 5s 检查、最多 3 次重试）
- 重试轨迹写入 `command_retry_log`
- 超过最大重试后状态标记为 `timeout`

## 修改须知

1. **新增 Python 端主题** — 在 `MqttBridge` 中添加订阅 + 处理逻辑
2. **新增硬件端主题** — 纳入 `MqttBridge` 订阅列表，补充持久化逻辑
3. **新增遥测字段** — 优先动态 key 透传，必要时补 `TelemetryTranslator` 映射
4. **新增能力字段** — 确保 capability topic 入库后能被 `DeviceService` 聚合查询
5. **修改任何主题** — 同步更新 README.md「MQTT 协议」章节
