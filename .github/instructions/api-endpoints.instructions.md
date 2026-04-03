---
description: "Use when adding, modifying, or debugging REST API endpoints, Controller methods, DTO definitions, or HTTP request/response handling in the HMHT backend."
applyTo: "src/main/java/ww/wangnong/web/**/*.java"
---

# REST API 端点约定

## 基本约定

| 项目 | 说明 |
|------|------|
| 基路径 | `/api`（`server.servlet.context-path`） |
| 响应格式 | `ApiResponse<T>(code, message, data)` |
| 成功响应 | `ApiResponse.ok(data)` 或 `ApiResponse.ok()` |
| 错误响应 | 抛 `BizException(code, message)` → `GlobalExceptionHandler` 自动捕获 |
| Token 提取 | `@RequestHeader(value = "Authorization", defaultValue = "") String auth` → `extractToken(auth)` |

## RESTful HTTP 方法语义

| 方法 | 用途 | 示例 |
|------|------|------|
| GET | 查询资源 | `GET /v1/nodes/{nodeId}` |
| POST | 创建资源 / 复杂查询 | `POST /v1/tasks`、`POST /v1/alerts/list` |
| PUT | 全量更新 | `PUT /v1/nodes/{nodeId}/sensor-thresholds` |
| DELETE | 删除资源 | `DELETE /v1/tasks/{taskId}` |

## DTO 定义规则

- **简单 DTO**：直接在 Controller 内用 Java record 定义请求体：
  ```java
  record LoginRequest(String username, String password) {}
  ```
- **复杂/共享 DTO**：在 `dto/` 包下定义独立类
- **不直接返回 Entity** — Service 接口内嵌套 record 作为返回类型：
  ```java
  public interface AuthService {
      record LoginResult(String token, long userId, String phone, String userName) {}
      LoginResult login(String username, String password);
  }
  ```
- JSON 字段命名需要 snake_case 时使用 `@JsonProperty("field_name")`

## 添加新端点的步骤

1. **在 Service 接口定义方法签名和嵌套 record**（`service/XxxService.java`）
2. **在 Mock 实现中提供方法实现**（`mock/MockXxxService.java`）
3. **如需 bridge 模式**，在 `bridge/` 中提供 bridge 实现
4. **在 Controller 中添加端点方法**：
   - 使用正确的 HTTP 方法注解（`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`）
   - 需要认证的端点添加 `@RequestHeader("Authorization")` 参数
   - 请求体用 Controller 内 record 接收
   - 返回 `ApiResponse.ok(service.method(...))`
5. **更新 README.md「REST API 端点清单」**

## 异常处理

```java
// Service 层抛出业务异常
throw new BizException(400, "手机号已注册");
throw new BizException(401, "密码错误");
throw new BizException(404, "节点不存在");

// GlobalExceptionHandler 自动捕获并返回：
// { "code": 400, "message": "手机号已注册", "data": null }
```

不要在 Controller 中捕获 `BizException`，让全局处理器统一处理。

## 现有端点快速参考

- **认证**：`AuthController` → `/v1/auth/login|register|send-verification-code|forgot-password|profile|password|avatar/use-fixed|logout`
- **节点**：`NodeController` → `/v1/nodes` (GET), `/v1/nodes/{id}` (GET), `/v1/nodes/{id}/sensor-meta|equipment-meta` (GET), `/v1/nodes/{id}/sensor-thresholds` (PUT)
- **传感器**：`SensorController` → `/v1/sensors/{nodeId}/history` (GET), `/v1/equipment/stats` (POST)
- **告警**：`AlertController` → `/v1/alerts/list|handle` (POST)
- **任务**：`ScheduledTaskController` → `/v1/tasks` CRUD + `/v1/tasks/{id}/toggle` (PUT)
- **AI 对话**：`ChatController` → `/v1/agri-qa/chat` (POST)
- **溯源/摄像头**：`ExtraController` → `/v1/traceability/scan` (POST), `/v1/cameras/{nodeId}/live|playback|keyframes` (GET)
- **管理**：`AdminController` → `/v1/admin/nodes` CRUD, `/v1/admin/users` 列表 + 角色 + 节点权限
- **操作日志**：`OperationLogController` → `/v1/admin/logs` (GET ?page=&size=&type=)
- **健康检查**：`HealthController` → `/v1/health` (GET)
- **基础设施**：`InfraController` → `/infra/health` (GET), `/infra/mqtt/publish` (POST)
