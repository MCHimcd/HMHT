---
description: "Use when modifying MQTT topics, bridge logic, telemetry translation, command forwarding, InfluxDB writes, PahoMqttService, MqttBridge, or adding new real-time data sources to the HMHT backend."
applyTo: "src/main/java/ww/wangnong/mqtt/**/*.java,src/main/java/ww/wangnong/bridge/**/*.java"
---

# MQTT 桥接层约定

## 架构概览

```
 Python (agri)                  HMHT (Java)                    App (HarmonyOS)
 ─────────────                  ───────────                    ───────────────
 /agri/device/+/telemetry  ──→  MqttBridge                ──→  /wangnong_2026/node/{nodeId}/telemetry
 /agri/ai_vision/alert     ──→  (TelemetryTranslator)     ──→  /wangnong_2026/node/{nodeId}/alert
                                  ↓
                                InfluxTelemetryWriter ──→ InfluxDB
                                 
 /agri/device/+/command    ←──  MqttBridge                ←──  /wangnong_2026/node/+/command
 /agri/device/+/ack        ──→  (日志记录)
```

## 主题契约

### HMHT 订阅的主题（bridge 模式）

| 主题 | 来源 | 处理 |
|------|------|------|
| `/agri/device/+/telemetry` | Python 后端 | TelemetryTranslator 翻译 → 发布到 App 主题 + 写入 InfluxDB |
| `/hmht/device/+/telemetry` | 硬件设备 | V2 规范化翻译 → 发布到 App 主题 + 写入 InfluxDB |
| `/hmht/device/+/capability` | 硬件设备 | 解析能力快照 → 持久化 capability/profile 明细 |
| `/hmht/device/+/ack` | 硬件设备 | 透传到 App ack topic + 回写 command_request/device_shadow |
| `/agri/ai_vision/alert` | Python AI 视觉 | 解析 vision_result → 创建 Alert → 发布到 App 告警主题 |
| `/wangnong_2026/node/+/command` | App 端 | 双路转发到 `/agri/device/{deviceId}/command` 与 `/hmht/device/{deviceId}/command` |
| `/agri/device/+/ack` | Python 后端 | 仅日志记录 |

### HMHT 发布的主题

| 主题 | 目标 | 内容 |
|------|------|------|
| `/wangnong_2026/node/{nodeId}/telemetry` | App 端 | 翻译后的遥测 JSON |
| `/wangnong_2026/node/{nodeId}/alert` | App 端 | 告警 JSON |
| `/agri/device/{deviceId}/command` | Python 后端 | 设备控制命令 |
| `/hmht/device/{deviceId}/command` | 硬件设备 | V2 设备控制命令 |
| `/wangnong_2026/node/{nodeId}/ack` | App 端 | 硬件命令回执 |

### 核心规则

- App 端保持通过后端桥接接收消息，不直接订阅 Python/硬件侧主题。
- **新增数据源应通过后端桥接**，不要修改 App 端订阅列表
- 修改主题时同步更新 README.md「MQTT 协议」章节

## PahoMqttService（MQTT 客户端）

### 生命周期
- `@PostConstruct` → 异步连接 + 自动重连
- `@PreDestroy` → 断开连接

### 核心方法

```java
// 发布消息
mqttService.publish(topic, payload, qos, retained);

// 订阅主题
mqttService.subscribe(topicFilter, qos, handler);
// handler: Consumer<InboundMessage>
// InboundMessage(topic, payload, qos, retained)

// 取消订阅
mqttService.unsubscribe(topicFilter);

// 连接状态
mqttService.isConnected();
```

### 规则

- **所有 MQTT 操作通过 PahoMqttService**，不直接创建 Paho 客户端实例
- 订阅回调线程安全（内部使用 `ConcurrentHashMap` 管理订阅）
- 支持通配符主题匹配（`+` 单层，`#` 多层）

## TelemetryTranslator（遥测翻译）

### 字段映射

| Python 字段名 | App 字段名 | 类型 |
|---------------|-----------|------|
| `air_temp` | `temperature` | Number |
| `air_humidity` | `humidity` | Number |
| `soil_temp` | `soilTemp` | Number |
| `soil_moisture` | `soilHumidity` | Number |
| `soil_ph` | `soilPh` | Number |
| `light_intensity` | `light` | Number |
| `co2_ppm` | `co2` | Number |
| `soil_n` | `soil_n` | Number |
| `soil_p` | `soil_p` | Number |
| `soil_k` | `soil_k` | Number |

### 结构转换

| Python 字段 | App 字段 |
|-------------|----------|
| `device_id` | `node_id` |
| `equipment_status` | `equipment` |
| `system_mode` | `mode` |

### 附加方法

- `extractEnvironment(translatedPayload)` — 提取环境传感器数据用于 InfluxDB
- `extractEquipment(translatedPayload)` — 提取设备状态数据用于 InfluxDB

## MqttBridge（桥接核心）

### AI 视觉告警处理

1. 解析 `/agri/ai_vision/alert` 消息中的 `vision_result`
2. 置信度 → 告警级别映射：
   - `≥ 0.85` → `critical`
   - `≥ 0.6` → `warning`
   - `< 0.6` → `info`
3. 疾病名称翻译（如 `Tomato_Early_blight` → `番茄早疫病`）
4. 创建 `AlertInfo` 并保存到数据库
5. 发布到 `/wangnong_2026/node/A03_Node_01/alert`

## InfluxDB 写入

### InfluxTelemetryWriter（bridge 专用）

- 由 MqttBridge 在遥测翻译后调用
- 写入动态 measurement：`telemetry_metric`（`node` + `metric` + `source`）
- 保留 legacy 兼容写入：`environment`（数值字段）
- 写入 `equipment_state`（状态字符串）用于设备统计
- 使用 Line Protocol 格式
- 自动转义 tag 值

### InfluxTelemetryService（通用写入）

- 位于 `influx/` 包，可被任何模块使用
- `writeEnvironment(nodeId, temperature, humidity, co2, light, soilTemp, soilHumidity)`
- `writeEquipmentState(nodeId, equipmentStates, mode)`
- 纳秒精度时间戳

### InfluxSensorService（bridge 查询）

- 查询聚合遥测数据（24h/7d/30d）
- 查询顺序：优先 `telemetry_metric`，空结果时回退 legacy `environment`
- 查询设备使用统计（7 天）
- 使用 SQL 查询：`date_bin()`, `AVG()`, `SUM()`
- **输入参数经过白名单校验防止 SQL 注入**

## ACK 重试闭环（bridge 模式）

- 命令下发后写入 `command_request`，ACK 到达写入 `command_ack` 并更新主状态。
- `CommandAckTracker` 定时扫描超时命令：默认 5 秒检查、最多 3 次重试。
- 重试轨迹写入 `command_retry_log`，超过最大重试后状态标记为 `timeout`。
- ACK 到达时更新 `device_shadow`，供 `/v1/devices/{deviceId}/shadow` 查询。

## 修改须知

1. **新增 Python 端主题**：在 `MqttBridge` 中添加订阅 + 处理逻辑，不修改 App 端
2. **新增硬件端主题**：优先纳入 `MqttBridge` 订阅列表，并补充对应持久化/审计逻辑
3. **新增遥测字段**：优先按动态 key 透传，必要时补 `TelemetryTranslator` 映射
4. **新增能力字段**：确保 capability topic 入库后能被 `DeviceService` 聚合查询
5. **新增告警类型**：更新 `MqttBridge` 中的告警解析和疾病名称映射
6. **修改任何主题**：同步更新 README.md「MQTT 协议」章节
