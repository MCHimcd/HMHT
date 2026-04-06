---
description: "Use when writing or editing Java source files in the HMHT backend. Covers dependency injection, Service interface/implementation patterns, mock/bridge dual-mode architecture, Lombok conventions, Repository patterns, and Spring configuration."
applyTo: "src/main/java/**/*.java"
---

# Spring Boot 编码模式

## Service 接口 + 双实现模式

每个业务领域包含三个层级：

```
service/XxxService.java          ← 接口定义（含嵌套 record DTO）
mock/MockXxxService.java         ← Mock 实现（默认激活）
bridge/BridgeXxxService.java     ← Bridge 实现（可选）
```

### 接口定义

```java
public interface XxxService {
    record XxxResult(String field1, int field2) {}
    XxxResult doSomething(String token, String param);
}
```

### Mock 实现

```java
@Service
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockXxxService implements XxxService {
    // 注入 Repository 和其他依赖
    private final XxxRepository xxxRepository;

    public MockXxxService(XxxRepository xxxRepository) {
        this.xxxRepository = xxxRepository;
    }

    @Override
    public XxxResult doSomething(String token, String param) {
        // 基于数据库的实现
    }
}
```

### Bridge 实现

```java
@Service
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "bridge")
public class BridgeXxxService implements XxxService {
    // 可能调用外部服务（Python API、InfluxDB 等）
}
```

**规则**：
- `matchIfMissing = true` 只加在 Mock 实现上（作为默认）
- 新增 Service 接口必须至少提供 Mock 实现
- Bridge 实现按需添加

## 依赖注入

**必须使用构造器注入**：

```java
// ✅ 正确
private final MyService myService;
private final MyRepository myRepository;

public MyComponent(MyService myService, MyRepository myRepository) {
    this.myService = myService;
    this.myRepository = myRepository;
}

// ❌ 禁止
@Autowired private MyService myService;
```

## Lombok 使用约定

| 场景 | 注解 | 说明 |
|------|------|------|
| Entity | `@Getter @Setter` | 不用 `@Data`（避免 equals/hashCode 陷阱） |
| DTO | Java record | 优先使用 record，不需要 Lombok |
| Builder 模式 | `@Builder` | 仅在 DTO 构建复杂时使用 |

## Repository 约定

```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    // 使用 Spring Data 约定命名查询
    Optional<Xxx> findByXxxId(String xxxId);
    List<Xxx> findByNodeId(String nodeId);
    
    // 复杂查询使用 @Query
    @Query("SELECT x FROM Xxx x WHERE x.userId = :userId ORDER BY x.createdAt DESC")
    List<Xxx> findByUserIdOrdered(@Param("userId") Long userId);
}
```

- Repository 继承 `JpaRepository<Entity, Long>`
- 简单查询用方法名推导
- 复杂查询用 `@Query` JPQL
- 命名：`XxxRepository`（与 Entity 名对应）

### V2 控制面查询约定（命令 ACK 重试）

- 批量扫描待处理命令时，优先使用受限结果集方法：`findTop{N}ByStatusInOrderBySentAtAsc(...)`
- 按设备维度查最近 ACK/影子时，优先使用 `findTopByDeviceIdOrderBy...Desc(...)`
- `PENDING / SENT / RETRYING` 状态集合查询必须显式排序，避免重试顺序漂移

## 配置类模式

### @ConfigurationProperties

```java
@Getter @Setter
@ConfigurationProperties(prefix = "hmht.xxx")
public class XxxProperties {
    private boolean enabled = true;
    private String url = "http://localhost:8080";
}
```

在 `HmhtApplication` 上有 `@ConfigurationPropertiesScan`，无需手动注册。

### 重试参数 Properties 建议

- 对调度器参数（如命令 ACK 超时重试）优先使用 `@ConfigurationProperties` 聚合配置
- 禁止在 `@Scheduled` 注解中硬编码毫秒值，统一通过属性占位符读取
- 变更默认值时需同步 README 与协议文档

### @Configuration + @ConditionalOnProperty

```java
@Configuration
@ConditionalOnProperty(name = "hmht.xxx.enabled", havingValue = "true", matchIfMissing = true)
public class XxxConfig {
    @Bean
    public XxxClient xxxClient(XxxProperties props) {
        return new XxxClient(props.getUrl());
    }
}
```

## 关键 Spring 注解速查

| 注解 | 用途 | 位置 |
|------|------|------|
| `@RestController` | REST 控制器 | Controller 类 |
| `@RequestMapping("/v1/xxx")` | 路径前缀 | Controller 类 |
| `@Service` | Service Bean | Service 实现类 |
| `@ConditionalOnProperty` | 条件加载 | Service/Config 类 |
| `@Entity` + `@Table` | JPA 实体 | Entity 类 |
| `@PostConstruct` / `@PreDestroy` | 生命周期回调 | 需要初始化/清理的 Bean |
| `@EnableScheduling` + `@Scheduled` | 定时任务 | 已在 HmhtApplication 启用 |

## 协议控制面职责边界

- `ProtocolPersistenceService`：负责命令请求、ACK、重试日志、设备能力快照、设备影子等持久化写入
- `CommandAckTracker`：负责超时扫描与重试调度，不直接承载协议翻译
- `MqttBridge`：负责 topic 收发与协议转换，避免直接操作多张控制面表

保持职责单一：桥接接入层（MQTT）与控制面持久化层（JPA）通过 Service 协作，不互相内联业务细节。

## JSON 解析依赖约束

- 若某 Service 仅做轻量 JSON 序列化/反序列化，优先在类内维护私有 `ObjectMapper`
- 仅在需要全局统一配置（模块注册、日期格式、命名策略）时再注入 Spring 管理的 `ObjectMapper` Bean

目的：减少测试上下文强依赖，避免 `contextLoads` 因 Bean 缺失失败。

## Token 认证模式

Controller 中需要认证的方法统一使用如下模式：

```java
@GetMapping("/xxx")
public ApiResponse<?> doSomething(
        @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
    return ApiResponse.ok(xxxService.doSomething(extractToken(auth)));
}

private String extractToken(String auth) {
    return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
}
```

Token 校验在 Service 层完成，Controller 只负责提取和传递。
