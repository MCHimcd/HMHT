# Sensor Equipment Specification

## Purpose

本规格记录当前模拟传感器历史、滚动缓存、低温告警触发和设备统计的已实现行为，包括时间范围采样、兼容请求字段、默认课程节点和随机演示数据生成。它只描述现有代码，不引入新需求，也不要求连接真实硬件或外部时序数据库。

## Requirements

### Requirement: Sensor History

系统 SHALL 生成课程演示用传感器历史曲线，不读取真实硬件或外部时序数据库。

#### Scenario: Query Sensor History

- **WHEN** 客户端请求 `GET /api/v1/sensors/{nodeId}/history`
- **THEN** `metric` 默认值为 `temperature`
- **AND** `range` 默认值为 `24h`
- **AND** 响应 `data.metric` 为请求指标
- **AND** 响应包含 `summary.max`、`summary.min`、`summary.avg` 和 `points`

#### Scenario: Range Sampling

- **WHEN** `range` 为 `24h`
- **THEN** 系统返回 48 个点，间隔 30 分钟，时间标签格式为 `HH:mm:ss`
- **WHEN** `range` 为 `7d`
- **THEN** 系统返回 42 个点，间隔 4 小时，时间标签格式为 `MM-dd HH:00`
- **WHEN** `range` 为 `30d`
- **THEN** 系统返回 30 个点，间隔 1 天，时间标签格式为 `MM-dd`
- **WHEN** `range` 为其他值
- **THEN** 系统按 `24h` 规格生成点

#### Scenario: Rolling History Cache

- **WHEN** 第一次请求某个 `nodeId + metric + range`
- **THEN** 系统初始化完整时间窗口
- **WHEN** 后续请求同一个 `nodeId + metric + range`
- **THEN** 系统移除最旧点并追加最新点
- **AND** 新点时间戳不早于当前时间，且大于上一窗口最后时间戳

#### Scenario: Metric Base Values

- **WHEN** `metric` 为当前模拟服务支持的指标
- **THEN** 系统使用该指标对应的基准值和波动范围生成随机值
- **WHEN** `metric` 不在模拟服务支持表内
- **THEN** 系统使用基准值 `25.0` 和波动范围 `3.0`

### Requirement: Low Temperature Alert

系统 SHALL 在 24 小时空气温度模拟流中按当前代码触发低温告警。

#### Scenario: Create Low Temperature Alert

- **WHEN** 请求 `metric=temperature` 且 `range=24h`
- **AND** 当前生成的最新值低于 `5.0`
- **AND** 同一节点未处于 60 秒低温告警冷却期
- **THEN** 系统写入一条类型为 `temperature_low`、级别为 `warning` 的告警
- **AND** 告警消息为 `模拟温室空气温度低于 5C，请检查保温设备。`

#### Scenario: Low Temperature Cooldown

- **WHEN** 同一节点在 60 秒冷却期内再次生成低温点
- **THEN** 系统不会重复创建低温告警

### Requirement: Equipment Statistics

系统 SHALL 生成课程演示用设备统计，不发送真实设备命令。

#### Scenario: Query Equipment Stats

- **WHEN** 客户端请求 `POST /api/v1/equipment/stats`
- **THEN** 系统从请求体读取 `node_id` 和 `equipment_key`
- **AND** 响应包含 `summary.total_value`、`summary.unit` 和 7 个最近日期统计点
- **AND** 每个统计点包含 `timeLabel` 和随机生成的一位小数 `value`

#### Scenario: Compatibility Request Fields

- **WHEN** 请求体包含历史兼容字段
- **THEN** `nodeId` 按 `node_id`、`query.node_id`、`device_id`、`query.device_id`、`SIM_Node_01` 的顺序取第一个非空值
- **AND** 设备 key 按 `equipment_key`、`query.equipment_key`、`query.equipment_type`、`query.device_type`、`water_pump` 的顺序取第一个非空值

#### Scenario: Equipment Units

- **WHEN** `equipmentKey` 为 `water_pump`
- **THEN** 统计单位为 `灌溶量(升)`
- **WHEN** `equipmentKey` 为 `led_light` 或 `fan`
- **THEN** 统计单位分别为 `补光时长(H)` 或 `运行时长(H)`
- **WHEN** `equipmentKey` 不在当前单位映射中
- **THEN** 统计单位为 `值`
