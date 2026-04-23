# HMHT 课程版后端开发约束

> HMHT 课程版是智慧农业系统的精简 Spring Boot 后端，只保留 REST API、MySQL 持久化、固定模拟节点和课程演示数据。不包含 MQTT、Python、硬件、InfluxDB 或多运行模式。

## 编码指令文件索引

| 文件 | 用途 | 适用范围 |
|------|------|----------|
| `instructions/java.instructions.md` | Java 编码规范、Service 模式、测试规范、API 约定 | `src/**/*.java` |
| `instructions/database.instructions.md` | Flyway 迁移、Entity 编写、Repository 模式 | `entity/`、`repository/`、`db/migration/` |

## 核心原则

1. **主动确认** — 需求不清时先提问，不基于猜测编写代码。
2. **测试同步** — 所有增删改操作必须同步更新测试。
3. **文档同步** — Controller、Service、Entity、环境变量、迁移、固定课程数据变化时，同步 `README.md` 与 `docs/api-endpoints.md`。
4. **Changelog 同步** — 每次修改完成后为 `CHANGELOG.md` 追加本次变更记录。

## 课程版边界

- 运行形态固定为课程版，不增加 `mock`、`bridge`、`hardware`、`full` 等运行模式。
- 固定模拟节点为 `SIM_Node_01`。
- 固定传感器类型为 `temperature`、`humidity`、`light`、`soilHumidity`。
- 固定设备类型为 `water_pump`、`led_light`、`fan`。
- 不引入 MQTT、Python、硬件、InfluxDB、设备能力快照、命令 ACK 闭环或外部时序数据。

## 代码风格要点

- **Java 版本**：Java 25。
- **依赖注入**：必须构造器注入，禁止字段 `@Autowired`。
- **DTO**：优先使用 Java `record`。
- **Entity**：`@Getter @Setter`，不用 `@Data`。
- **响应格式**：统一 `ApiResponse<T>`，业务错误抛 `BizException`。

## 最终验证

Java 变更必须通过：

```bash
./gradlew clean bootJar
./gradlew test
```
