# HMHT 后端开发约束

> HMHT 是鸿蒙慧田智慧农业系统的 Spring Boot 后端，提供 REST API、MQTT 桥接和数据持久化，支持 mock/bridge/hardware/full 四种运行模式。

## 编码指令文件索引

| 文件 | 用途 | 适用范围 |
|------|------|----------|
| `instructions/java.instructions.md` | Java 编码规范、Service 模式、测试规范、API 约定 | `src/**/*.java` |
| `instructions/database.instructions.md` | Flyway 迁移、Entity 编写、Repository 模式 | `entity/`、`repository/`、`db/migration/` |
| `instructions/mqtt.instructions.md` | MQTT 主题契约、桥接层约定、遥测映射 | `mqtt/`、`bridge/` |

## 核心原则

1. **主动确认** — 需求不清时先提问，不基于猜测编写代码
2. **测试先行** — 所有增删改操作必须先写/更新测试，再实现业务逻辑
3. **README 同步** — 代码变更后按下表同步更新 README.md 对应章节

### README 同步清单

| 变更类型 | 需更新的 README 章节 |
|----------|---------------------|
| 新增/删除/重命名 Controller | 「REST API」汇总表 + `docs/api-endpoints.md` |
| 新增/删除/修改 Service 接口 | 「服务清单」 |
| 新增/删除/修改 Entity | 「数据模型清单」 |
| 新增/修改 MQTT 主题 | 「MQTT 协议」 |
| 修改环境变量 | 「环境变量」 |
| 修改数据库迁移 | 「数据库迁移」 |
| 修改运行模式逻辑 | 「运行模式」 |

## 代码风格要点

- **依赖注入**：必须构造器注入，禁止 `@Autowired` 字段注入
- **条件装配**：使用 `config/condition/` 包中的自定义注解（`@ConditionalOnMockMode` / `@ConditionalOnBridgeMode` / `@ConditionalOnPythonRoute` / `@ConditionalOnHardwareRoute` / `@ConditionalOnNoPythonRoute`），不使用旧的 SpEL 表达式
- **DTO**：优先使用 Java record
- **Entity**：`@Getter @Setter`（不用 `@Data`）
- **响应格式**：统一 `ApiResponse<T>`，错误抛 `BizException`

## 最终验证

每次修改完成后必须通过：

### Java 变更

```bash
./gradlew clean bootJar  # 编译
./gradlew test           # 测试
```

### 非 Java 变更（仅脚本/文档/配置）

- 可跳过 Gradle 构建和测试
- 执行与改动相关的验证（如脚本自检）

**只有编译和测试都通过，才视为修改完成。**
