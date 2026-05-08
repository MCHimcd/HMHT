---
description: "Database rules for HMHT entities, repositories, migrations, and seed data"
applyTo: "src/main/java/ww/wangnong/entity/**/*.java,src/main/java/ww/wangnong/repository/**/*.java,src/main/resources/db/**"
---

# Database Instructions

## Scope

适用于 JPA Entity、Spring Data Repository、Flyway 迁移和课程种子数据。

## Flyway

迁移目录：

```text
src/main/resources/db/migration/
```

命名格式：

```text
V{N}__{description}.sql
```

- `N` 为递增整数。
- 双下划线 `__` 必须保留。
- `description` 使用简短英文 `snake_case`。
- 课程版从 `V1__course_schema.sql` 开始维护全新精简 schema。
- 已发布结构变更通过新增迁移脚本完成，不直接改历史迁移。

## Entity

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

| Area | Rule |
|------|------|
| Lombok | `@Getter @Setter`，不用 `@Data` |
| Primary key | `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| Table/column names | `snake_case` |
| Timestamps | `@CreationTimestamp` / `@UpdateTimestamp` |
| Associations | 默认 `FetchType.LAZY`，显式 `@JoinColumn` |

## Repository

```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    Optional<Xxx> findByXxxId(String xxxId);

    List<Xxx> findByNodeId(String nodeId);

    boolean existsByPhone(String phone);
}
```

## Seed Data

种子入口：`EssentialDataSeeder`。

| Item | Value |
|------|-------|
| Node | `SIM_Node_01` |
| Sensor keys | `temperature`, `humidity`, `light`, `soilHumidity` |
| Equipment keys | `water_pump`, `led_light`, `fan` |

修改种子数据时，同步 Entity、迁移脚本、`README.md`、`docs/api-endpoints.md` 和相关前端文档。

## New Table Checklist

1. 新增 Flyway 迁移脚本。
2. 新增 Entity。
3. 新增 Repository。
4. 新增 Repository 测试。
5. 如果课程演示需要默认数据，更新 `EssentialDataSeeder`。
6. 同步 `README.md` 的 Data Model 和 `docs/api-endpoints.md`。
