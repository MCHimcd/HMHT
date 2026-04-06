---
description: "Use when modifying local run scripts, run-mode switching, startup parameters, local environment variables, or start.ps1 behavior in HMHT."
applyTo: "start.ps1,scripts/tests/start.ps1.test.ps1,README.md,src/main/resources/application.properties,src/test/resources/application.properties"
---

# 本地开发约定

## 适用范围

本文件用于规范本地开发入口、运行模式、脚本参数和相关文档同步。

## 运行入口

- Java 单服务（mock）：`./gradlew bootRun`
- 脚本入口（支持 mock/bridge/hardware/full）：`./start.ps1 -Mode mock|bridge|hardware|full`
- 启动脚本依赖 Windows Terminal（`wt.exe`）

## 运行模式约定

| 模式 | 说明 | 典型场景 |
|------|------|----------|
| `mock` | 仅 Java 服务，使用 mock 数据 | 接口联调、前端本地开发 |
| `bridge` | App <-> Java <-> Python（不走硬件） | Python 对接联调、AI 对话联调 |
| `hardware` | App <-> Java <-> 硬件（不走 Python） | 纯硬件链路联调 |
| `full` | App <-> Java <-> Python <-> 硬件 | 全链路端到端验证 |

修改 `hmht.run.mode` 或 `HMHT_RUN_MODE` 时，需同步检查：

- Service 条件装配（`@ConditionalOnProperty`）是否符合预期
- 多值模式条件（如 `@ConditionalOnExpression`）是否与模式语义一致
- README 中「运行模式」「构建与运行」章节是否同步
- 相关测试（尤其是模式切换行为）是否仍成立

修改命令重试配置 `hmht.command.retry.*` 或 `HMHT_COMMAND_RETRY_*` 时，需同步检查：

- `README.md` 中环境变量表是否同步
- `docs/backend-hardware-protocol.md` 中默认超时/重试策略是否同步
- `src/test/java/**/CommandAckTrackerTest` 与相关桥接持久化测试是否覆盖新行为

## 命令重试参数约定

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `hmht.command.retry.check-interval-ms` | `5000` | ACK 超时扫描周期 |
| `hmht.command.retry.initial-delay-ms` | `3000` | 调度器首次启动延迟 |
| `hmht.command.retry.max-retries` | `3` | 单条命令最大重试次数 |
| `hmht.command.retry.default-timeout-ms` | `5000` | 命令未显式指定 timeout 时的默认超时 |
| `hmht.command.retry.batch-size` | `200` | 单轮扫描处理上限 |

建议：

- 本地联调优先调低 `check-interval-ms`，不要先调高 `max-retries`
- 若出现重复下发，先检查设备 ACK 幂等与 broker 延迟，再调整重试参数

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
  - 若改动包含 `hmht.command.retry.*`，执行 `./gradlew test --tests "*CommandAckTrackerTest"`
  - README 文案与实际脚本参数一致性检查
