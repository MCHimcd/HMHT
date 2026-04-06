---
description: >
  Use when writing, editing, or reviewing test files in the HMHT backend.
  Covers JUnit 5 / Mockito patterns, Spring Boot test slices, test data
  factories, naming conventions, and the test-first development mandate.
applyTo: "src/test/**/*.java"
---

# HMHT 测试规范

## 测试先行原则

**任何增删改操作，都必须先编写 / 更新测试，再实现或修改业务代码。**

## 测试分层

| 层级 | 注解 | 说明 |
|------|------|------|
| Controller | `@WebMvcTest(XxxController.class)` | MockMvc + `@MockBean` 服务层 |
| Service | `@ExtendWith(MockitoExtension.class)` | `@Mock` Repository + `@InjectMocks` MockXxxService |
| Repository | `@DataJpaTest` | H2 内存数据库自动创建 schema |
| Scheduler/Bridge Loop | `@ExtendWith(MockitoExtension.class)` | 直接调用定时方法，验证重试与状态迁移 |
| 工具类 | 纯 JUnit 5 | 无 Spring 上下文（如 TelemetryTranslator） |
| DTO / Exception | 纯 JUnit 5 | ApiResponse record、BizException、GlobalExceptionHandler |

## 命名规范

- 测试类：`{被测类}Test.java`，放在与被测类相同包路径下（`src/test/java/ww/wangnong/...`）
- 测试方法：`{methodName}_{scenario}_{expectedResult}`
  ```java
  // 示例
  void login_wrongPassword_throwsBizException401()
  void findByPhone_existingUser_returnsUser()
  void listAlerts_emptyQuery_returnsAll()
  ```

## 测试数据工厂

使用 `TestDataFactory`（`src/test/java/ww/wangnong/TestDataFactory.java`）来创建测试 Entity：

```java
User user = TestDataFactory.user("13800000001", "测试用户");
Node node = TestDataFactory.node("N01", "温室1号", user);
Alert alert = TestDataFactory.alert("ALT-001", "N01");
```

- 工厂方法仅设置必填字段和关键属性，其余字段用合理默认值
- 需要自定义其他字段时，在工厂方法返回后手动 set

## Controller 测试 (@WebMvcTest)

```java
@WebMvcTest(XxxController.class)
class XxxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XxxService xxxService;

    @Test
    void endpoint_scenario_expectedResult() throws Exception {
        // given
        when(xxxService.someMethod(any())).thenReturn(expectedResult);

        // when & then
        mockMvc.perform(get("/v1/xxx")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.field").value("expected"));
    }
}
```

重要事项：
- Spring Boot 4.x 推荐使用 `@MockitoBean`（非 `@MockBean`）
- 不需要 `@MockitoBean PahoMqttService`（SecurityConfig 不依赖它）
- InfraController 需要 `@MockitoBean PahoMqttService`（构造器注入）
- `BizException` 由 `GlobalExceptionHandler` 拦截并返回 `ApiResponse.error()`
- 使用 `@Import(GlobalExceptionHandler.class)` 确保异常处理器生效（如需要）
- 请求体使用 JSON 字符串，`contentType(MediaType.APPLICATION_JSON)`
- Authorization header 格式：`"Bearer {token}"`，Controller 内部 extractToken 去掉前缀

V2 新增测试建议：

- 设备接口：覆盖 `/v1/devices/{deviceId}/capability` 与 `/shadow`。
- 重试闭环：覆盖超时、重试、最大次数终态、ACK 终止重试。
- 动态时序：覆盖 `telemetry_metric` 查询与 legacy 回退。

## Service 测试 (@ExtendWith MockitoExtension)

```java
@ExtendWith(MockitoExtension.class)
class MockXxxServiceTest {

    @Mock private SomeRepository someRepo;
    @Mock private OperationLogService opLog;
    @InjectMocks private MockXxxService service;

    @Test
    void method_scenario_expectedResult() {
        // given
        when(someRepo.findById(1L)).thenReturn(Optional.of(entity));

        // when
        var result = service.someMethod(args);

        // then
        assertThat(result).isNotNull();
        verify(someRepo).save(any());
    }
}
```

重要事项：
- MockAuthService 需要直接实例化（不用 @InjectMocks），因为它是其他 Mock 服务依赖的
- MockNodeService、MockAdminService 等注入 MockAuthService 实例
- 使用 AssertJ `assertThat()` 风格断言
- 验证 BizException 场景：`assertThatThrownBy(() -> ...).isInstanceOf(BizException.class).extracting("code").isEqualTo(401)`

## Repository 测试 (@DataJpaTest)

```java
@DataJpaTest
class XxxRepositoryTest {

    @Autowired
    private XxxRepository repo;

    @Autowired
    private TestEntityManager em;

    @Test
    void customQueryMethod_scenario_expectedResult() {
        // given - 通过 em.persistAndFlush() 创建测试数据
        Entity entity = TestDataFactory.someEntity(...);
        em.persistAndFlush(entity);

        // when
        var result = repo.customQueryMethod(args);

        // then
        assertThat(result).hasSize(1);
    }
}
```

重要事项：
- 对有外键关系的 Entity，需先 persist 依赖的 Entity（如 Node 依赖 User）
- `@DataJpaTest` 自动回滚事务，不需手动清理
- 只测试自定义查询方法，不测试 JPA 内置的 `findById` / `save` 等

## 测试配置

测试用 `src/test/resources/application.properties`：
- H2 内存数据库 (`MODE=MySQL`)
- `spring.jpa.hibernate.ddl-auto=create-drop`
- `spring.flyway.enabled=false`
- `hmht.mqtt.enabled=false`
- `hmht.influxdb.enabled=false`

补充建议：

- 新增 Service 若依赖非业务核心 Bean（如 JSON 工具），优先避免让 `@SpringBootTest` 因上下文缺 Bean 失败。
- 若必须依赖该 Bean，请在测试配置中显式提供或在构造器中注入可替代实现。

## 常见 BizException 状态码

| 状态码 | 含义 | 常见场景 |
|--------|------|----------|
| 400 | 错误请求 | 验证码错误、旧密码错误 |
| 401 | 未认证 | token 无效或过期、用户名密码错误 |
| 403 | 无权限 | 非管理员访问管理接口、非节点所有者 |
| 404 | 不存在 | 节点不存在、用户不存在、任务不存在 |
| 409 | 冲突 | 手机号已注册、节点ID已存在 |
