---
description: "Use when modifying local run scripts, run-mode switching, startup parameters, local environment variables, or start.ps1 behavior in HMHT."
applyTo: "start.ps1,scripts/tests/start.ps1.test.ps1,README.md,src/main/resources/application.properties,src/test/resources/application.properties"
---

# 本地开发约定

## 适用范围

本文件用于规范本地开发入口、运行模式、脚本参数和相关文档同步。

## 运行入口

- Java 单服务（mock）：`./gradlew bootRun`
- 脚本入口（支持 mock/bridge）：`./start.ps1 -Mode mock|bridge`
- 启动脚本依赖 Windows Terminal（`wt.exe`）

## 运行模式约定

| 模式 | 说明 | 典型场景 |
|------|------|----------|
| `mock` | 仅 Java 服务，使用 mock 数据 | 接口联调、前端本地开发 |
| `bridge` | Java + Python 桥接链路 | MQTT 桥接联调、端到端验证 |

修改 `hmht.run.mode` 或 `HMHT_RUN_MODE` 时，需同步检查：

- Service 条件装配（`@ConditionalOnProperty`）是否符合预期
- README 中「运行模式」「构建与运行」章节是否同步
- 相关测试（尤其是模式切换行为）是否仍成立

## start.ps1 修改约定

修改脚本行为时，必须关注以下点：

- 参数兼容性：避免破坏现有参数（`-Mode`、`-PythonDir`、`-JavaDir`、`-AgriChatPort`、`-InfluxDir`、`-InfluxToken`）
- 统一停机语义：Ctrl+C 先优雅停止，再超时强制回收
- 状态面板输出：保持服务状态展示（Starting/Running/Stopping/Stopped/Failed）
- Worker 模式入参与主控模式协同，不引入循环启动

## 文档同步规则

当改动涉及本地启动方式、脚本参数、运行模式时，必须同步更新 README：

- 「运行模式」
- 「构建与运行」

## 验证要求（非 Java 改动）

当改动仅限脚本/文档/本地配置且不涉及 Java 源码与 Gradle 构建逻辑时：

- 可跳过 `./gradlew clean bootJar`
- 可跳过 `./gradlew test`
- 需执行与改动直接相关的验证：
  - `powershell -ExecutionPolicy Bypass -File .\scripts\tests\start.ps1.test.ps1`
  - README 文案与实际脚本参数一致性检查
