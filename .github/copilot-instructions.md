# HMHT (万农智慧系统后端) — AI 编码指令

## 主动确认原则

**若需求描述不清晰、存在歧义，或无法确定实现方式，必须先向用户提出问题进行确认，再动手实现。** 不要基于猜测编写代码。

## 测试先行原则

**所有增删改操作，必须先编写 / 更新对应的测试用例，再实现业务逻辑。**

- 新增功能 → 先写失败测试 → 实现功能 → 测试通过
- 修改功能 → 先更新/新增测试以覆盖变更 → 修改代码 → 确认测试通过
- 删除功能 → 确认测试中无残留引用 → 删除代码 → 删除对应测试

## README 同步规则

- **修改代码前**：先阅读 `README.md` 了解项目架构、约定和现有清单。
- **修改代码后**：如果变更涉及以下内容，**必须同步更新 README.md 对应章节**：
  - 新增 / 删除 / 重命名 Controller → 更新「Controller 清单」和「REST API 端点清单」
  - 新增 / 删除 / 修改 REST 端点 → 更新「REST API 端点清单」
  - 新增 / 删除 / 修改 Entity → 更新「数据模型清单」
  - 新增 / 删除 / 修改 Service 接口或实现 → 更新「服务清单」
  - 新增 / 修改 MQTT 主题或桥接逻辑 → 更新「MQTT 协议」章节
  - 修改 Flyway 迁移脚本 → 更新「数据库迁移」章节
  - 修改安全配置 → 更新「安全架构」章节
  - 修改环境变量或运行模式 → 更新「运行模式」章节
  - 修改构建配置或 Docker 编排 → 更新「构建与运行」章节
  - 修改与 agri 项目的交互 → 更新「与 agri 项目的协作」章节

## Java 编码约束

1. **构造器注入** — 使用构造器注入依赖，不在字段上使用 `@Autowired`：
   ```java
   // ❌
   @Autowired private MyService myService;
   
   // ✅
   private final MyService myService;
   public MyController(MyService myService) { this.myService = myService; }
   ```

2. **DTO vs Entity 分离** — Controller 不直接返回 Entity，使用 record 或 DTO 类承载接口数据。Service 接口中 record 嵌套定义用于简单 DTO。

3. **Lombok 使用约定** — Entity 用 `@Getter @Setter`，不用 `@Data`（避免 equals/hashCode 问题）。DTO 优先使用 Java record。

4. **Service 双实现模式** — 新增 Service 接口后，必须提供 Mock 实现（`mock/` 包），bridge 实现按需。通过 `@ConditionalOnProperty("hmht.run.mode")` 切换：
   ```java
   @Service
   @ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
   public class MockXxxService implements XxxService { ... }
   ```

5. **统一响应** — 所有 Controller 方法返回 `ApiResponse<?>`，成功用 `ApiResponse.ok(data)`，业务错误抛 `BizException(code, message)`。

6. **Token 提取** — Controller 通过 `@RequestHeader("Authorization")` 获取并用 `extractToken()` 去除 `Bearer ` 前缀，传给 Service 做业务级校验。

## 项目架构约定

- **统一响应**：`ApiResponse<T>(code, message, data)` — 200 成功，非 200 业务错误。
- **全局异常**：`BizException` → `GlobalExceptionHandler` → `ApiResponse.error()`。
- **Flyway 迁移**：文件名 `V{N}__{description}.sql`，放在 `src/main/resources/db/migration/`。
- **配置结构化**：MQTT 用 `MqttProperties`，InfluxDB 用 `InfluxDbProperties`，均通过 `@ConfigurationProperties` 绑定。
- **MQTT 统一入口**：所有 MQTT 操作通过 `PahoMqttService`，不直接创建 Paho 客户端实例。
- **InfluxDB 写入**：bridge 模式用 `InfluxTelemetryWriter`，通用写入用 `InfluxTelemetryService`，不直接调用 InfluxDB 客户端。

## MQTT 协议约定

- HMHT 作为桥接层，订阅 Python 端 `/agri/device/+/telemetry` 和 `/agri/ai_vision/alert`。
- 翻译后发布到 App 端 `/wangnong_2026/node/{nodeId}/telemetry` 和 `/wangnong_2026/node/{nodeId}/alert`。
- App 端命令 `/wangnong_2026/node/{nodeId}/command` 转发到 `/agri/device/{deviceId}/command`。
- **新增订阅主题时**需同步更新 `MqttBridge` 订阅列表、README MQTT 章节。

## 最终验证（必须全部通过）

**每次修改完成后，必须依次通过以下两项验证，缺一不可：**

### 1. 编译验证

```bash
./gradlew clean bootJar
```
- 成功标志：`BUILD SUCCESSFUL`
- 失败：搜索 `error:` 或 `FAILURE` 定位问题

### 2. 测试验证

```bash
./gradlew test
```
- 成功标志：`BUILD SUCCESSFUL` 且测试报告无失败
- 测试报告：`build/reports/tests/test/index.html`

### 验证流程

```
修改代码 → 编译通过 (BUILD SUCCESSFUL) → 测试全部通过 → 完成
         ↘ 编译失败 → 修复错误 → 重新编译
                    ↘ 测试失败 → 修复用例或代码 → 重新测试
```

**只有编译和测试都通过，才视为修改完成。**
