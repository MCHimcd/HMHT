# HMHT 后端-硬件通讯协议与数据格式（V2）

> 状态：Draft
> 
> 版本：2.0.0
> 
> 生效范围：HMHT Backend <-> Hardware Device

## 1. 目标与范围

本文档定义 HMHT 后端与硬件设备之间的 MQTT 通讯契约，用于支撑以下目标：

- 优先打通 App <-> 后端 <-> 硬件链路（telemetry、command、ack）。
- 支持不定量传感器与设备（动态 key），避免后端为每个新 key 改代码。
- 通过协议版本与兼容规则，确保演进可控、可回滚。
- 明确 MySQL 与 InfluxDB 的落库映射，保证可追溯和可观测。

不在本规范范围：

- App 与后端 REST API 细节。
- Python 侧历史 topic/payload 适配细节。

## 2. Topic 契约

默认 QoS=1，retained=false。

| 方向 | Topic | 说明 |
|------|-------|------|
| Device -> Backend | `/hmht/device/{deviceId}/telemetry` | 设备上报遥测数据 |
| Backend -> Device | `/hmht/device/{deviceId}/command` | 后端下发控制命令 |
| Device -> Backend | `/hmht/device/{deviceId}/ack` | 设备对 command 的回执 |
| Device -> Backend | `/hmht/device/{deviceId}/capability` | 设备能力声明/更新（推荐） |
| Device -> Backend | `/hmht/device/{deviceId}/heartbeat` | 设备心跳（可选） |

约束：

- `deviceId` 由设备唯一标识，建议与后端设备注册主键一致。
- 新增业务消息类型优先复用既有 topic，通过 `msg_type` 扩展。

## 3. 通用消息信封

所有消息均采用统一信封：

```json
{
  "msg_type": "telemetry",
  "protocol_version": "2.0",
  "device_id": "A03_Node_01",
  "timestamp": 1765000000123,
  "trace_id": "6f3f6d2b-b0bb-4e3b-86f8-7af0744a9b7c",
  "payload": {},
  "extensions": {}
}
```

字段定义：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `msg_type` | string | 是 | 消息类型：`telemetry`/`command`/`ack`/`capability`/`heartbeat` |
| `protocol_version` | string | 否 | 协议版本，缺失时按 `1.0` 兼容处理 |
| `device_id` | string | 是 | 设备唯一 ID |
| `timestamp` | number | 是 | 设备侧毫秒时间戳（Unix epoch ms） |
| `trace_id` | string | 是 | 全链路追踪 ID（UUID 推荐） |
| `payload` | object | 是 | 消息主体 |
| `extensions` | object | 否 | 扩展字段容器，未知字段建议写入此处 |

## 4. Telemetry 数据格式

### 4.1 Payload 结构

```json
{
  "sensors": {
    "air_temp": {
      "value": 26.4,
      "unit": "C",
      "quality": "good"
    },
    "soil_moisture": {
      "value": 41.2,
      "unit": "%"
    },
    "custom_sensor_x": {
      "value": 123.45,
      "unit": "ppm"
    }
  },
  "equipments": {
    "water_pump": {
      "state": "on"
    },
    "fan": {
      "state": "off"
    },
    "custom_device_y": {
      "state": "standby"
    }
  },
  "mode": "manual"
}
```

### 4.2 规则

- `sensors` 与 `equipments` 支持不定量 key。
- 后端必须允许未知 key 透传、落库和查询，不可直接丢弃。
- 推荐 key 使用小写蛇形命名：`[a-z0-9_]+`。
- `mode` 为可选字段，推荐值：`manual`/`auto`/`smart`。

### 4.3 单项定义

`sensors.<sensor_key>`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `value` | number/string/bool | 是 | 传感器值（优先 number） |
| `unit` | string | 否 | 单位 |
| `quality` | string | 否 | 数据质量，如 `good`/`bad`/`estimated` |

`equipments.<equipment_key>`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `state` | string/bool | 是 | 设备状态，如 `on`/`off`/`standby` |
| `params` | object | 否 | 设备参数快照 |

### 4.4 与 App 对齐的最终键名（推荐标准）

说明：

- 硬件侧 `payload.sensors` 使用下表左列键名上报，后端桥接后会转换为 App 侧最终键名（右列）。
- 硬件侧 `payload.equipments` 与 App 侧 `data.equipment` 当前为同名透传（无重命名）。
- 未知 key 允许透传，但若希望 App 有稳定展示与单位，建议优先使用本节标准键名。

#### 4.4.1 sensors 键名映射（硬件 -> App）

| 硬件上报 key (`payload.sensors`) | App 最终 key (`data.environment`) | 建议单位 | 说明 |
|---|---|---|---|
| `air_temp` | `temperature` | `C` | 空气温度 |
| `air_humidity` | `humidity` | `%` | 空气湿度 |
| `soil_temp` | `soilTemp` | `C` | 土壤温度 |
| `soil_moisture` | `soilHumidity` | `%` | 土壤湿度 |
| `soil_ph` | `soilPh` | （无） | 土壤酸碱度 |
| `light_intensity` | `light` | `Lux` | 光照强度 |
| `co2_ppm` | `co2` | `ppm` | 二氧化碳浓度 |
| `soil_n` | `soilN` | `mg/kg`（建议） | 土壤氮 |
| `soil_p` | `soilP` | `mg/kg`（建议） | 土壤磷 |
| `soil_k` | `soilK` | `mg/kg`（建议） | 土壤钾 |

建议：

- 统一采用 `snake_case` 的硬件 key（如 `air_temp`）。
- 若上报自定义传感器（如 `custom_sensor_x`），可透传；但 App 若需统一图标/单位/排序，需在 capability 中声明并补充前端元数据。

#### 4.4.2 equipments 键名（硬件 = App）

以下设备 key 可直接与 App 对齐使用：

| `payload.equipments` key | App `data.equipment` key | 典型含义 |
|---|---|---|
| `water_pump` | `water_pump` | 水泵 |
| `water_valve` | `water_valve` | 水阀 |
| `led_light` | `led_light` | 补光灯 |
| `fan` | `fan` | 风机 |
| `camera` | `camera` | 摄像头 |
| `heater` | `heater` | 加热器 |
| `misting` | `misting` | 喷雾 |
| `co2_generator` | `co2_generator` | 二氧化碳发生器 |
| `fertilizer` | `fertilizer` | 施肥装置 |

`state` 建议值：

- 开关型：`on` / `off`
- 多态设备：`standby` / `running` / `fault`（也可用布尔值，后端会转字符串入库）

## 5. Command 数据格式

### 5.1 Payload 结构

```json
{
  "command_id": "CMD-20260406-0001",
  "target_key": "water_pump",
  "action": "turn_on",
  "params": {
    "duration_sec": 30
  },
  "timeout_ms": 5000
}
```

### 5.2 规则

- `command_id` 全局唯一（建议业务前缀 + 时间 + 序号）。
- `target_key`、`action` 由设备 capability 声明决定。
- 设备应在 `timeout_ms` 内返回 ack（超时由后端重试策略处理）。
- 后端默认重试策略：`timeout_ms=5000`（缺省）、扫描周期 5 秒、最大重试 3 次，超过后标记 `timeout`。

## 6. ACK 数据格式

### 6.1 Payload 结构

```json
{
  "command_id": "CMD-20260406-0001",
  "status": "executed",
  "ack_code": 0,
  "ack_message": "ok",
  "acked_at": 1765000001456,
  "detail": {
    "target_key": "water_pump",
    "action": "turn_on"
  }
}
```

### 6.2 status

- `accepted`：设备已接收，待执行。
- `executed`：执行成功。
- `rejected`：拒绝执行。
- `failed`：执行失败。

### 6.3 ack_code 建议分层

| ack_code | 含义 |
|----------|------|
| 0 | 成功 |
| 1001 | 不支持的 `target_key` |
| 1002 | 不支持的 `action` |
| 1003 | 参数非法 |
| 2001 | 设备忙 |
| 2002 | 执行超时 |
| 3001 | 设备内部错误 |

## 7. Capability 数据格式（推荐）

```json
{
  "sensors": {
    "air_temp": {"unit": "C", "data_type": "number", "min": -40, "max": 120},
    "soil_moisture": {"unit": "%", "data_type": "number", "min": 0, "max": 100}
  },
  "equipments": {
    "water_pump": {"actions": ["turn_on", "turn_off"], "controllable": true},
    "fan": {"actions": ["turn_on", "turn_off"], "controllable": true}
  },
  "firmware": {
    "version": "1.2.3",
    "vendor": "wan-nong"
  }
}
```

用途：

- 后端动态构建设备能力视图。
- App 按 capability 渲染传感器卡片和控制面板。

## 8. 版本协商与兼容策略

- 版本格式：`major.minor`，如 `2.0`。
- 后端兼容策略：
  - 缺失 `protocol_version` -> 按 `1.0` 处理。
  - `major` 不兼容时进入兼容分支或拒收并写死信。
  - 新增字段不得破坏旧字段语义。
- 兼容窗口：建议至少保留两个小版本并行解析能力。

## 9. MySQL 落库映射（控制面）

建议新增/重构表（命名可在迁移脚本中最终确定）：

| 消息/能力 | 表 | 说明 |
|-----------|----|------|
| 设备注册基础信息 | `device_profile` | 设备元信息、在线状态、固件信息 |
| 能力快照 | `device_capability` | 当前能力版本与摘要 |
| 传感器能力明细 | `capability_sensor` | 每个 `sensor_key` 的类型、单位、范围 |
| 设备能力明细 | `capability_equipment` | 每个 `equipment_key` 的动作与控制能力 |
| 命令请求 | `command_request` | command 下发记录与当前状态 |
| ACK 回执 | `command_ack` | ack 明细记录，按 `command_id` 关联 |
| 重试记录 | `command_retry_log` | 超时/失败后的重试轨迹 |
| 设备影子 | `device_shadow` | 最近状态快照（设备状态快速读取） |
| 接入审计 | `ingest_audit` | 每条消息接收与处理流水（含 trace_id） |
| 死信队列 | `ingest_deadletter` | 无法解析/校验失败消息及原因 |

## 10. InfluxDB 落库映射（时序面）

推荐 measurements：

- `telemetry_metric`
- `equipment_state`

`telemetry_metric`：

- tags：`node`, `metric`, `source`
- fields：`value`
- time：写入时按 Influx 精度（ms）对齐

说明：

- `node` 与协议中的 `device_id` 语义一致（后端桥接层统一映射）。
- `source` 当前约定：`hw_v2` / `agri_v1`。
- 为兼容历史查询，后端仍保留 `environment` 的 legacy 双写。

`equipment_state`：

- tags：`node`, `equipment`
- fields：`status`
- time：写入时按 Influx 精度（ms）对齐

规则：

- 新增 sensor/equipment key 不需要改 measurement 结构。
- 未识别 key 必须可写入并可查询。

## 11. 校验与处理等级

| 等级 | 场景 | 处理 |
|------|------|------|
| 严格失败 | 缺失 `msg_type`/`device_id`/`timestamp`/`payload` | 拒收并写 `ingest_deadletter` |
| 告警放行 | `protocol_version` 过旧但可兼容 | 记录告警并继续处理 |
| 透传放行 | 未知 sensor/equipment key | 正常写入时序并记录统计 |

## 12. 端到端示例

### 12.1 Telemetry 正常

Topic: `/hmht/device/A03_Node_01/telemetry`

```json
{
  "msg_type": "telemetry",
  "protocol_version": "2.0",
  "device_id": "A03_Node_01",
  "timestamp": 1765000100123,
  "trace_id": "8af2d0c5-0a5a-45d7-9ad9-78ac11f16f9b",
  "payload": {
    "sensors": {
      "air_temp": {"value": 25.8, "unit": "C"},
      "soil_moisture": {"value": 39.1, "unit": "%"}
    },
    "equipments": {
      "water_pump": {"state": "off"}
    },
    "mode": "manual"
  }
}
```

### 12.2 Command 正常

Topic: `/hmht/device/A03_Node_01/command`

```json
{
  "msg_type": "command",
  "protocol_version": "2.0",
  "device_id": "A03_Node_01",
  "timestamp": 1765000110000,
  "trace_id": "0d5d6dd2-3e69-4aac-9476-ae15cccb57f2",
  "payload": {
    "command_id": "CMD-20260406-0009",
    "target_key": "water_pump",
    "action": "turn_on",
    "params": {"duration_sec": 60},
    "timeout_ms": 5000
  }
}
```

### 12.3 ACK 失败

Topic: `/hmht/device/A03_Node_01/ack`

```json
{
  "msg_type": "ack",
  "protocol_version": "2.0",
  "device_id": "A03_Node_01",
  "timestamp": 1765000111200,
  "trace_id": "0d5d6dd2-3e69-4aac-9476-ae15cccb57f2",
  "payload": {
    "command_id": "CMD-20260406-0009",
    "status": "rejected",
    "ack_code": 1003,
    "ack_message": "invalid duration_sec",
    "acked_at": 1765000111198,
    "detail": {"min": 1, "max": 30}
  }
}
```

## 13. 演进约束

- 不得在同一 `major` 版本内重定义字段语义。
- 新增字段必须是向后兼容（可忽略）语义。
- 协议变更必须同步更新：独立协议文档、README 摘要入口、迁移脚本、契约测试。
