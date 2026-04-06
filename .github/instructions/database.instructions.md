---
description: "Use when modifying JPA Entity definitions, Flyway migration scripts, Repository interfaces, or database schema in the HMHT backend. Covers Entity conventions, migration naming, table structure, and MockDataLifecycle seed data."
applyTo: "src/main/java/ww/wangnong/entity/**/*.java,src/main/java/ww/wangnong/repository/**/*.java,src/main/resources/db/**"
---

# 数据库约定

## Flyway 迁移

### 命名规范

```
V{N}__{description}.sql
```

- `N` — 版本号（递增整数：1, 2, 3...）
- `__` — **双下划线**分隔
- `description` — 简短英文描述（snake_case）
- 示例：`V2__add_notification_table.sql`

### 规则

- **不修改已执行的迁移脚本** — 修改已部署的脚本会导致 Flyway 校验失败
- 结构变更（新增表、列、索引）通过新增迁移脚本实现
- 迁移目录：`src/main/resources/db/migration/`
- JPA DDL 策略为 `update`，Flyway 负责创建基础表结构，Hibernate 可补充新增列

### 当前迁移

- `V1__init.sql` — 初始化 13 张表（users, nodes, sensor_type, node_sensor, equipment_type, node_equipment, alerts, command_logs, chat_messages, verification_codes, scheduled_tasks, operation_logs, user_node_access）
- `V2__device_protocol_control_plane.sql` — 新增协议控制面 10 张表（device_profile, device_capability, capability_sensor, capability_equipment, command_request, command_ack, command_retry_log, device_shadow, ingest_audit, ingest_deadletter）

## JPA Entity 编写约定

### 基本结构

```java
@Entity
@Table(name = "xxx_table")
@Getter
@Setter
public class XxxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "some_field", nullable = false, length = 50)
    private String someField;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 规则

| 规则 | 说明 |
|------|------|
| 注解 | `@Getter @Setter`（不用 `@Data`） |
| 主键 | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| 表名 | snake_case（`@Table(name = "xxx_table")`） |
| 列名 | snake_case（`@Column(name = "some_field")`） |
| 时间戳 | `@CreationTimestamp` / `@UpdateTimestamp` 由 Hibernate 自动管理 |
| 唯一约束 | `@Column(unique = true)` 或 `@Table(uniqueConstraints = ...)` |
| 关联关系 | `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn`，**默认懒加载** |
| 默认值 | 在字段上直接赋值：`private Boolean enabled = true;` |

### 字段类型映射

| Java 类型 | MySQL 类型 | 注解 |
|-----------|------------|------|
| `String` | `VARCHAR(N)` | `@Column(length = N)` |
| `String` (长文本) | `TEXT` | `@Column(columnDefinition = "TEXT")` |
| `Long` | `BIGINT` | 默认 |
| `Integer` | `INT` | 默认 |
| `Double` | `DOUBLE` | 默认 |
| `Boolean` | `TINYINT(1)` | 默认 |
| `LocalDateTime` | `DATETIME` | 默认 |
| `LocalDate` | `DATE` | 默认 |

## 现有表结构概览

| 表名 | 主要业务键 | 说明 |
|------|-----------|------|
| `users` | `phone` (unique) | 用户，角色 user/admin |
| `nodes` | `node_id` (unique) | 农业节点，关联 user_id |
| `sensor_type` | `sensor_key` (unique) | 全局传感器类型定义 |
| `node_sensor` | `(node_id, sensor_key)` unique | 节点传感器 + 阈值 |
| `equipment_type` | `equipment_key` (unique) | 全局设备类型定义 |
| `node_equipment` | `(node_id, equipment_key)` unique | 节点设备绑定 |
| `alerts` | `alert_id` (unique) | 告警记录 |
| `command_logs` | — | 命令执行日志 |
| `chat_messages` | — | AI 对话消息，关联 user_id |
| `verification_codes` | — | 短信验证码 |
| `scheduled_tasks` | — | 定时设备控制任务 |
| `operation_logs` | — | 操作审计日志 |
| `user_node_access` | `(user_id, node_id)` unique | 用户-节点访问权限 |
| `device_profile` | `device_id` (unique) | 设备档案（V2） |
| `device_capability` | `(device_id, created_at)` index | 设备能力快照（V2） |
| `capability_sensor` | `(device_id, capability_version, sensor_key)` unique | 传感器能力明细（V2） |
| `capability_equipment` | `(device_id, capability_version, equipment_key)` unique | 设备能力明细（V2） |
| `command_request` | `command_id` (unique) | 命令主状态（V2） |
| `command_ack` | `command_id` index | 命令回执流水（V2） |
| `command_retry_log` | `(command_id, created_at)` index | 命令重试轨迹（V2） |
| `device_shadow` | `device_id` (unique) | 设备影子快照（V2） |
| `ingest_audit` | `(msg_type, created_at)` index | 接入审计（V2） |
| `ingest_deadletter` | `(msg_type, created_at)` index | 死信队列（V2） |

## Repository 约定

```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    // 按业务键查询
    Optional<Xxx> findByXxxId(String xxxId);
    
    // 按关联查询
    List<Xxx> findByNodeId(String nodeId);
    
    // 存在性检查
    boolean existsByPhone(String phone);
    
    // 删除
    void deleteByNodeId(String nodeId);
}
```

V2 控制面常见查询模式：

- 最新快照：`findTopByDeviceIdOrderByCreatedAtDesc(...)`
- 扫描待处理命令：`findTop200ByStatusInOrderBySentAtAsc(List.of("sent", "retry"))`
- 影子按设备查：`findByDeviceId(...)`
- 重试统计：`countByCommandId(...)`

## MockDataLifecycle 注意事项

- **激活条件**：`hmht.run.mode=mock`（默认模式）
- **启动行为**：若 `users` 表为空则播种全部演示数据（包括 InfluxDB 30 天历史），否则只重建 5 条种子告警
- **关闭行为**（`@PreDestroy`）：TRUNCATE mock + V2 控制面相关表（禁用外键检查），**会清除所有数据**
- **安全门控**：通过 `hmht.mock.seed.enabled` 属性控制，防止生产环境意外触发
- 修改种子数据时注意保持与 Entity 定义和 V1__init.sql 表结构的一致性

## 新增表步骤

1. 创建 Flyway 迁移脚本 `V{N}__{description}.sql`
2. 创建 JPA Entity 类（`entity/` 包）
3. 创建 Repository 接口（`repository/` 包）
4. 如 mock 模式需要种子数据，更新 `MockDataLifecycle`
5. 更新 README.md「数据模型清单」
