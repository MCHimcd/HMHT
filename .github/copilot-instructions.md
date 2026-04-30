# HMHT Course Edition Instructions

## Purpose

HMHT 课程版是智慧农业系统的精简实现：Spring Boot 后端、Vue 3 前端、MySQL 持久化、Flyway 迁移、固定模拟节点和课程演示数据。

## Instruction Files

| File | Scope |
|------|-------|
| `.github/instructions/java.instructions.md` | Java 编码、Service 模式、API 响应、测试 |
| `.github/instructions/database.instructions.md` | Entity、Repository、Flyway 迁移、种子数据 |
| `AGENTS.md` | Codex 入口规则、文档同步和验证要求 |

## Project Boundaries

- 固定课程版运行形态，不增加 `mock`、`bridge`、`hardware`、`full` 等运行模式。
- 固定节点：`SIM_Node_01`。
- 固定传感器类型：`temperature`、`humidity`、`light`、`soilHumidity`。
- 固定设备类型：`water_pump`、`led_light`、`fan`。
- 不引入 MQTT、Python、真实硬件、InfluxDB、设备能力快照、命令 ACK 闭环或外部时序数据。

## Workflow Rules

- 需求不清时先确认，再实现。
- 增删改业务行为时，同步新增或更新测试。
- Java 变更最终运行 `./gradlew clean bootJar` 和 `./gradlew test`。
- 前端变更按 `frontend/package.json` 运行对应构建或测试脚本。
- 每次完成修改后，在 `CHANGELOG.md` 追加变更记录。

## Documentation Sync

以下内容发生变化时，同步 `README.md`、`docs/api-endpoints.md` 和相关 `docs/*.md`：

- Controller 路径、请求体、响应体或认证要求
- Service 行为、业务错误码或课程数据
- Entity、Repository、Flyway 迁移或默认环境变量
- 固定账号、节点、传感器类型、设备类型
- 前端路由、请求封装、运行脚本或代理方式

## Java Style

- Java 版本：25。
- 依赖注入：构造器注入，禁止字段 `@Autowired`。
- DTO：优先使用 Java `record`。
- Entity：使用 `@Getter @Setter`，不用 `@Data`。
- Controller：统一返回 `ApiResponse<T>`。
- 业务错误：抛出 `BizException`，由 `GlobalExceptionHandler` 处理。
