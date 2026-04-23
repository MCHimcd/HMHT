---
description: "Java 编码规范 - 课程版 Spring Boot、测试规范、API 约定"
applyTo: "src/**/*.java"
---

# Java 编码规范

## 依赖注入

**必须使用构造器注入**，不使用 `@Autowired` 字段注入：

```java
private final MyService myService;

public MyController(MyService myService) {
    this.myService = myService;
}
```

## Service 接口 + 实现模式

```java
// 接口定义（含嵌套 record DTO）
public interface XxxService {
    record XxxResult(String field1, int field2) {}
    XxxResult doSomething(String token, String param);
}

// 实现
@Service
public class DatabaseXxxService implements XxxService {
    // ...
}
```

课程版只有一种运行形态，不添加条件装配注解或运行模式分支。

## Lombok 约定

| 场景 | 注解 |
|------|------|
| Entity | `@Getter @Setter`（不用 `@Data`） |
| DTO | Java record（优先） |

## Entity & Repository

Entity 和 Repository 编写约定见 `database.instructions.md`。

## REST API 约定

- 基路径：`/api`
- 响应格式：`ApiResponse<T>(code, message, data)`
- 成功：`ApiResponse.ok(data)`
- 错误：抛 `BizException(code, message)` → `GlobalExceptionHandler` 自动捕获

```java
@GetMapping("/xxx")
public ApiResponse<?> doSomething(
        @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
    return ApiResponse.ok(xxxService.doSomething(extractToken(auth)));
}
```

## 测试规范

### 测试先行

**任何增删改操作，都必须先编写/更新测试，再实现业务代码。**

### 测试分层

| 层级 | 注解 | 说明 |
|------|------|------|
| Controller | `@WebMvcTest` | MockMvc + `@MockitoBean` 服务层 |
| Service | `@ExtendWith(MockitoExtension.class)` | `@Mock` + `@InjectMocks` |
| Repository | `@DataJpaTest` | H2 内存数据库 |

### 命名规范

- 测试类：`{被测类}Test.java`
- 测试方法：`{methodName}_{scenario}_{expectedResult}`

### Controller 测试

```java
@WebMvcTest(XxxController.class)
class XxxControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private XxxService xxxService;

    @Test
    void endpoint_scenario_expectedResult() throws Exception {
        when(xxxService.someMethod(any())).thenReturn(expectedResult);

        mockMvc.perform(get("/v1/xxx")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

### Service 测试

```java
@ExtendWith(MockitoExtension.class)
class MockXxxServiceTest {
    @Mock private SomeRepository someRepo;
    @InjectMocks private MockXxxService service;

    @Test
    void method_scenario_expectedResult() {
        when(someRepo.findById(1L)).thenReturn(Optional.of(entity));
        var result = service.someMethod(args);
        assertThat(result).isNotNull();
    }
}
```

### Repository 测试

```java
@DataJpaTest
class XxxRepositoryTest {
    @Autowired private XxxRepository repo;
    @Autowired private TestEntityManager em;

    @Test
    void customQueryMethod_scenario_expectedResult() {
        Entity entity = TestDataFactory.someEntity(...);
        em.persistAndFlush(entity);
        var result = repo.customQueryMethod(args);
        assertThat(result).hasSize(1);
    }
}
```

## 常见 BizException 状态码

| 状态码 | 含义 | 场景 |
|--------|------|------|
| 400 | 错误请求 | 验证码错误、旧密码错误 |
| 401 | 未认证 | token 无效、密码错误 |
| 403 | 无权限 | 非管理员访问、非节点所有者 |
| 404 | 不存在 | 节点/用户/任务不存在 |
| 409 | 冲突 | 手机号已注册、节点 ID 已存在 |
