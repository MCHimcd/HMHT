---
description: "Java rules for HMHT course-edition Spring Boot code"
applyTo: "src/**/*.java"
---

# Java Instructions

## Scope

适用于 `src/**/*.java`。数据库实体、仓库和迁移相关规则见 `database.instructions.md`。

## Dependency Injection

只使用构造器注入，不使用字段 `@Autowired`。

```java
private final MyService myService;

public MyController(MyService myService) {
    this.myService = myService;
}
```

## Service Pattern

- Service 接口放业务契约和面向 API 的 `record` DTO。
- 数据库实现使用 `DatabaseXxxService`。
- 课程版只有一种运行形态，不增加条件装配或运行模式分支。

```java
public interface XxxService {
    record XxxResult(String field1, int field2) {
    }

    XxxResult doSomething(String token, String param);
}

@Service
public class DatabaseXxxService implements XxxService {
}
```

## DTO And Lombok

| Type | Rule |
|------|------|
| DTO | 优先使用 Java `record` |
| Entity | 使用 `@Getter @Setter` |
| Entity | 不使用 `@Data` |

## REST API

- API 基路径由 `server.servlet.context-path=/api` 提供。
- Controller 路径使用 `/v1/...`。
- 成功响应：`ApiResponse.ok(data)`。
- 业务错误：抛 `BizException(code, message)`。
- 全局异常由 `GlobalExceptionHandler` 转换为统一响应。

```java
@GetMapping("/xxx")
public ApiResponse<?> doSomething(
        @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
    return ApiResponse.ok(xxxService.doSomething(extractToken(auth)));
}
```

## Tests

业务增删改必须同步更新测试。优先按变更层级选择最小有效测试。

| Layer | Tooling |
|-------|---------|
| Controller | `@WebMvcTest` + MockMvc + `@MockitoBean` |
| Service | `@ExtendWith(MockitoExtension.class)` + Mockito |
| Repository | `@DataJpaTest` + `TestEntityManager` |
| E2E | Spring Boot integration test when cross-layer behavior changes |

命名：

- 测试类：`{TargetClass}Test.java`
- 测试方法：`{methodName}_{scenario}_{expectedResult}`

## Common Business Codes

| Code | Meaning | Typical Case |
|------|---------|--------------|
| `400` | Bad request | 验证码错误、旧密码错误、参数错误 |
| `401` | Unauthorized | token 无效、密码错误 |
| `403` | Forbidden | 非管理员访问、非节点授权用户 |
| `404` | Not found | 节点、用户、任务不存在 |
| `409` | Conflict | 手机号已注册、节点 ID 已存在 |

## Verification

Java 变更完成后运行：

```bash
./gradlew clean bootJar
./gradlew test
```
