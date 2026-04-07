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

- **不修改已执行的迁移脚本** — 修改已部署的脚本会导致 Flyway 校验失败
- 结构变更通过新增迁移脚本实现
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

V2 控制面常见查询模式：
- 最新快照：`findTopByDeviceIdOrderByCreatedAtDesc(...)`
- 扫描待处理命令：`findTop200ByStatusInOrderBySentAtAsc(...)`

## MockDataLifecycle 注意事项

- **激活条件**：`@ConditionalOnMockMode`
- **启动行为**：若 `users` 表为空则播种全部演示数据
- **关闭行为**（`@PreDestroy`）：TRUNCATE mock + V2 控制面相关表
- 修改种子数据时注意保持与 Entity 定义和迁移脚本的一致性

## 新增表步骤

1. 创建 Flyway 迁移脚本 `V{N}__{description}.sql`
2. 创建 JPA Entity 类（`entity/` 包）
3. 创建 Repository 接口（`repository/` 包）
4. 如 mock 模式需要种子数据，更新 `MockDataLifecycle`
5. 更新 README.md「数据模型清单」
