---
description: "数据库约定 - Flyway 迁移、Entity 编写、Repository 模式"
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

### 规则

- 课程版数据库从 `V1__course_schema.sql` 开始，作为全新精简 schema 维护。
- 课程版发布后，结构变更通过新增迁移脚本实现。
- 迁移目录：`src/main/resources/db/migration/`

## JPA Entity 编写约定

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
| 表名/列名 | snake_case |
| 时间戳 | `@CreationTimestamp` / `@UpdateTimestamp` |
| 关联关系 | `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn` |

## Repository 约定

```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    Optional<Xxx> findByXxxId(String xxxId);
    List<Xxx> findByNodeId(String nodeId);
    boolean existsByPhone(String phone);
}
```

## 课程版种子数据

- 种子入口：`EssentialDataSeeder`
- 固定节点：`SIM_Node_01`
- 固定传感器类型：`temperature`、`humidity`、`light`、`soilHumidity`
- 固定设备类型：`water_pump`、`led_light`、`fan`
- 修改种子数据时注意保持 Entity、迁移脚本、README 和 `docs/api-endpoints.md` 一致。

## 新增表步骤

1. 创建 Flyway 迁移脚本 `V{N}__{description}.sql`
2. 创建 JPA Entity 类（`entity/` 包）
3. 创建 Repository 接口（`repository/` 包）
4. 编写 Repository 测试（测试规范见 `java.instructions.md`）
5. 如课程演示需要种子数据，更新 `EssentialDataSeeder`
6. 更新 README.md「Data Model」和 `docs/api-endpoints.md`
