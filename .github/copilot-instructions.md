# HMHT 后端开发约束

## 核心原则

1. **主动确认** — 需求不清时先提问，不基于猜测编写代码
2. **测试先行** — 所有增删改操作必须先写/更新测试，再实现业务逻辑
3. **README 同步** — 修改 Controller/Service/Entity/MQTT/数据库后必须同步更新 README 对应章节

## 运行模式条件装配

使用项目自定义条件注解（位于 `config/condition/`）：

- `@ConditionalOnMockMode` — 仅 mock 模式（默认）
- `@ConditionalOnBridgeMode` — bridge/hardware/full 模式
- `@ConditionalOnPythonRoute` — bridge/full 模式（启用 Python 路由）
- `@ConditionalOnHardwareRoute` — hardware/full 模式（启用硬件路由）
- `@ConditionalOnNoPythonRoute` — mock/hardware 模式（不启用 Python 路由）

**不要使用旧的 SpEL 表达式**，统一使用上述注解。

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
