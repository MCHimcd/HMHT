---
description: "Use when debugging compilation errors, runtime failures, MQTT/HTTP connection issues, database problems, or test failures in the HMHT Spring Boot backend. Covers Gradle build diagnostics, Spring Boot startup failures, common exceptions, and systematic troubleshooting."
applyTo: "src/**/*.java"
---

# 调试与排查指南

## Gradle 构建失败

### 编译命令

```bash
./gradlew clean bootJar
```

### 输出关键字解读

| 关键字 | 含义 | 操作 |
|--------|------|------|
| `BUILD SUCCESSFUL` | 编译成功 | 无需操作 |
| `BUILD FAILED` | 编译失败 | 搜索 `error:` 定位问题 |
| `error:` | 编译错误 | 修复后重新编译 |
| `FAILURE:` | Gradle 任务失败 | 查看上方错误详情 |
| `warning:` | 编译警告 | 通常可忽略 |

### 常见编译错误

| 错误 | 原因 | 修复 |
|------|------|------|
| `cannot find symbol` | 缺少 import / 类名拼错 | 检查 import 语句和类名 |
| `incompatible types` | 类型不匹配 | 检查方法返回值和参数类型 |
| `method does not override` | 接口方法签名不匹配 | 对照 Service 接口方法签名 |
| `no suitable constructor` | 缺少构造器参数 | 确认依赖注入参数完整 |
| `java: package xxx does not exist` | 缺少依赖 | 检查 build.gradle 依赖声明 |

## Spring Boot 启动失败

### 常见启动错误

| 错误 | 原因 | 修复 |
|------|------|------|
| `BeanCreationException` | Bean 创建失败 | 检查构造器注入参数、@ConditionalOnProperty 配置 |
| `NoSuchBeanDefinitionException` | 找不到 Bean | 确认 @Service/@Component 注解和包扫描范围 |
| `BeanCurrentlyInCreationException` | 循环依赖 | 重构代码消除循环引用 |
| `No qualifying bean of type 'ObjectMapper'` | 新增 Service 依赖了上下文未提供的 Bean | 对仅本地 JSON 解析场景，优先在类内创建私有 ObjectMapper；或显式提供 Bean |
| `DataSource` 相关异常 | MySQL 连接失败 | 检查 MySQL 是否运行、URL/用户名/密码是否正确 |
| `FlywayValidateException` | 迁移脚本变更 | 不要修改已执行的迁移脚本，新增 V{N+1}__xxx.sql |
| `Port already in use: 8080` | 端口占用 | `netstat -ano | findstr :8080` 找进程并终止 |

### 排查步骤

1. 阅读完整的异常堆栈，找到 `Caused by:` 根因
2. 检查 `application.properties` 中相关配置
3. 如果是 Bean 相关问题，检查 `@ConditionalOnProperty` 条件是否匹配当前 `hmht.run.mode`
4. 如果是数据库问题，确认 MySQL 服务运行中且 `hmht` 数据库存在

## 数据库问题

| 错误 | 原因 | 修复 |
|------|------|------|
| `Table 'hmht.xxx' doesn't exist` | Flyway 未执行 / 表未创建 | 检查 V1__init.sql 是否正确、Flyway 是否启用 |
| `LazyInitializationException` | Session 已关闭时访问懒加载属性 | 在 Service 层 join fetch 或改用 DTO 投影 |
| `DataIntegrityViolationException` | 唯一约束冲突 | 检查 unique 字段是否重复 |
| `TransactionRequiredException` | 写操作无事务 | 确认 Service 方法有 `@Transactional` |
| `Lock wait timeout` | 死锁或长事务 | 检查事务范围，缩小锁定时间 |

## MQTT 连接问题

### 排查清单

1. **检查 MQTT Broker 可达性**：`hmht.mqtt.server-uri` 指向 `tcp://47.109.27.231:1883`
2. **检查连接状态**：`GET /api/infra/health` → `mqttConnected: true/false`
3. **检查客户端 ID 冲突**：多实例运行时 `hmht-server` 可能冲突，确保唯一
4. **检查日志**：搜索 `PahoMqttService` 和 `MqttBridge` 日志输出
5. **检查主题订阅**：bridge 模式需订阅 `/agri/device/+/telemetry` 和 `/agri/ai_vision/alert`
6. **检查 V2 主题**：硬件链路需确认 `/hmht/device/+/telemetry`、`/hmht/device/+/capability`、`/hmht/device/+/ack`
7. **检查重试循环**：确认 `hmht.command.retry.*` 参数是否合理，避免误判重试风暴

### 使用 InfraController 测试

```bash
# 检查 MQTT 状态
curl http://localhost:8080/api/infra/health

# 手动发布消息
curl -X POST http://localhost:8080/api/infra/mqtt/publish \
  -H "Content-Type: application/json" \
  -d '{"topic": "/test/topic", "payload": "hello", "qos": 1}'
```

## HTTP 请求问题

### 排查清单

1. **基路径**：所有请求需加 `/api` 前缀（如 `http://localhost:8080/api/v1/auth/login`）
2. **Content-Type**：请求体使用 `application/json`
3. **Authorization**：需认证的端点携带 `Bearer {token}` 头
4. **CORS**：当前全开放，不应有跨域问题

## 日志配置

当前日志输出到 `logs/hmht.log`，开发环境使用 DEBUG 级别。

### 关键日志前缀

| 包/类 | 日志内容 |
|--------|----------|
| `ww.wangnong.mqtt.PahoMqttService` | MQTT 连接、订阅、消息收发 |
| `ww.wangnong.bridge.MqttBridge` | 桥接消息翻译与转发 |
| `ww.wangnong.bridge.TelemetryTranslator` | 遥测字段映射 |
| `ww.wangnong.mock.MockDataLifecycle` | 种子数据播种与清理 |
| `org.flywaydb` | 数据库迁移执行 |
| `org.hibernate.SQL` | JPA SQL 语句 |

## 测试失败排查

### 命令

```bash
./gradlew test
```

### 测试报告

- HTML 报告：`build/reports/tests/test/index.html`
- XML 结果：`build/test-results/test/`

### 常见问题

| 问题 | 原因 | 修复 |
|------|------|------|
| `ApplicationContextError` | 测试上下文加载失败 | 检查 `src/test/resources/application.properties` 配置 |
| H2 SQL 语法错误 | H2 与 MySQL 语法差异 | 使用 H2 兼容模式或调整 SQL |
| Bean 冲突 | Mock/Bridge 实现同时加载 | 测试中用 `@TestPropertySource` 指定 `hmht.run.mode` |
| contextLoads 因依赖缺失失败 | 新增组件引入了额外 Bean 依赖 | 为组件减小依赖面或在测试上下文提供替身 Bean |
