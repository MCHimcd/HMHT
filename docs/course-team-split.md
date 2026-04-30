# 课程版五人分工明细

本分工面向 HMHT 课程版单体 Spring Boot 后端。五个人在同一个仓库中协作，最终组合为一个可运行应用。运行形态固定为 Java 25 + MySQL + `SIM_Node_01` 模拟节点，不包含 MQTT、Python、硬件、InfluxDB 或多运行模式。

## 分工原则

- 每个文件只有一个主负责人，避免多人同时修改同一文件。
- 公共接口变更必须提前同步给相关负责人。
- Controller、Service、Entity、Repository、测试和文档必须一起维护。
- 每次合并前至少运行 `./gradlew test`；集成负责人最终运行 `./gradlew clean bootJar`。

## 1 号：工程基础、启动配置、数据库基线和操作日志

负责 Java 25 工程、课程版启动、固定种子数据、数据库迁移、健康检查、操作日志和集成 E2E 基础。

| 文件 | 职责 |
|------|------|
| `.gitattributes` | 仓库文本/二进制属性 |
| `.gitignore` | 忽略规则 |
| `.github/workflows/deploy.yml` | CI/CD Java 25 构建配置 |
| `AGENTS.md` | Codex 项目级约束 |
| `build.gradle` | Gradle 插件、Java 25 toolchain、依赖、JaCoCo |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 版本 |
| `gradlew` | Unix Gradle 启动脚本 |
| `gradlew.bat` | Windows Gradle 启动脚本 |
| `settings.gradle` | Gradle 项目名称 |
| `src/main/java/ww/wangnong/HmhtApplication.java` | Spring Boot 启动入口 |
| `src/main/java/ww/wangnong/config/EssentialDataSeeder.java` | 课程版固定种子数据 |
| `src/main/java/ww/wangnong/config/SecurityConfig.java` | Spring Security、BCrypt、CORS |
| `src/main/java/ww/wangnong/entity/OperationLog.java` | 操作日志实体 |
| `src/main/java/ww/wangnong/repository/OperationLogRepository.java` | 操作日志仓库 |
| `src/main/java/ww/wangnong/service/OperationLogService.java` | 操作日志服务 |
| `src/main/java/ww/wangnong/web/HealthController.java` | 课程版健康检查 |
| `src/main/java/ww/wangnong/web/OperationLogController.java` | 操作日志 REST API |
| `src/main/resources/META-INF/additional-spring-configuration-metadata.json` | 配置元数据 |
| `src/main/resources/application.properties` | 默认运行配置 |
| `src/main/resources/application-prod.properties` | 生产 profile 配置 |
| `src/main/resources/db/migration/V1__course_schema.sql` | 课程版 Flyway schema |
| `src/main/resources/logback-spring.xml` | 日志配置 |
| `src/test/java/ww/wangnong/HmhtApplicationTests.java` | 应用上下文测试 |
| `src/test/java/ww/wangnong/config/ApplicationLoggingPropertiesTest.java` | 日志配置测试 |
| `src/test/java/ww/wangnong/config/EssentialDataSeederTest.java` | 种子数据测试 |
| `src/test/java/ww/wangnong/config/SecurityConfigTest.java` | 安全配置测试 |
| `src/test/java/ww/wangnong/e2e/AuthNodeE2eTest.java` | 登录后访问节点的 E2E 测试 |
| `src/test/java/ww/wangnong/e2e/E2eTestBase.java` | 登录和验证码 E2E 公共工具 |
| `src/test/java/ww/wangnong/repository/OperationLogRepositoryTest.java` | 操作日志仓库测试 |
| `src/test/java/ww/wangnong/service/OperationLogServiceTest.java` | 操作日志服务测试 |
| `src/test/java/ww/wangnong/web/HealthControllerTest.java` | 健康检查测试 |
| `src/test/java/ww/wangnong/web/OperationLogControllerTest.java` | 操作日志 Controller 测试 |
| `src/test/resources/application.properties` | 测试配置 |

## 2 号：用户认证与个人资料

负责验证码、注册、登录、个人资料、密码、登出和业务 token 校验。

| 文件 | 职责 |
|------|------|
| `src/main/java/ww/wangnong/entity/User.java` | 用户实体 |
| `src/main/java/ww/wangnong/entity/VerificationCode.java` | 验证码实体 |
| `src/main/java/ww/wangnong/repository/UserRepository.java` | 用户仓库 |
| `src/main/java/ww/wangnong/repository/VerificationCodeRepository.java` | 验证码仓库 |
| `src/main/java/ww/wangnong/service/AuthService.java` | 认证服务接口 |
| `src/main/java/ww/wangnong/service/impl/DatabaseAuthService.java` | 认证服务实现 |
| `src/main/java/ww/wangnong/web/AuthController.java` | 认证 REST API |
| `src/test/java/ww/wangnong/repository/UserRepositoryTest.java` | 用户仓库测试 |
| `src/test/java/ww/wangnong/repository/VerificationCodeRepositoryTest.java` | 验证码仓库测试 |
| `src/test/java/ww/wangnong/TestDataFactory.java` | 认证和业务测试共享实体工厂 |
| `src/test/java/ww/wangnong/service/impl/DatabaseAuthServiceTest.java` | 认证服务测试 |
| `src/test/java/ww/wangnong/web/AuthControllerTest.java` | 认证 Controller 测试 |

## 3 号：模拟节点、固定传感器和固定设备类型

负责 `SIM_Node_01`、固定传感器字典、固定设备字典、节点授权、节点元数据和阈值更新。

| 文件 | 职责 |
|------|------|
| `src/main/java/ww/wangnong/entity/Node.java` | 节点实体 |
| `src/main/java/ww/wangnong/entity/SensorType.java` | 固定传感器类型实体 |
| `src/main/java/ww/wangnong/entity/EquipmentType.java` | 固定设备类型实体 |
| `src/main/java/ww/wangnong/entity/NodeSensor.java` | 节点传感器绑定与阈值 |
| `src/main/java/ww/wangnong/entity/NodeEquipment.java` | 节点设备绑定 |
| `src/main/java/ww/wangnong/entity/UserNodeAccess.java` | 用户节点授权 |
| `src/main/java/ww/wangnong/repository/NodeRepository.java` | 节点仓库 |
| `src/main/java/ww/wangnong/repository/SensorTypeRepository.java` | 传感器类型仓库 |
| `src/main/java/ww/wangnong/repository/EquipmentTypeRepository.java` | 设备类型仓库 |
| `src/main/java/ww/wangnong/repository/NodeSensorRepository.java` | 节点传感器仓库 |
| `src/main/java/ww/wangnong/repository/NodeEquipmentRepository.java` | 节点设备仓库 |
| `src/main/java/ww/wangnong/repository/UserNodeAccessRepository.java` | 用户节点授权仓库 |
| `src/main/java/ww/wangnong/service/NodeService.java` | 节点服务接口 |
| `src/main/java/ww/wangnong/service/impl/DatabaseNodeService.java` | 节点服务实现 |
| `src/main/java/ww/wangnong/web/NodeController.java` | 节点 REST API |
| `src/test/java/ww/wangnong/repository/NodeRepositoryTest.java` | 节点仓库测试 |
| `src/test/java/ww/wangnong/repository/UserNodeAccessRepositoryTest.java` | 节点授权仓库测试 |
| `src/test/java/ww/wangnong/service/impl/DatabaseNodeServiceTest.java` | 节点服务测试 |
| `src/test/java/ww/wangnong/web/NodeControllerTest.java` | 节点 Controller 测试 |

## 4 号：传感器模拟数据、设备统计、告警、统一响应和异常处理

负责传感器模拟数据、设备统计、告警列表、告警处理、统一响应和异常处理。

| 文件 | 职责 |
|------|------|
| `src/main/java/ww/wangnong/entity/Alert.java` | 告警实体 |
| `src/main/java/ww/wangnong/dto/ApiResponse.java` | 统一响应结构 |
| `src/main/java/ww/wangnong/exception/BizException.java` | 业务异常 |
| `src/main/java/ww/wangnong/exception/GlobalExceptionHandler.java` | 全局异常响应 |
| `src/main/java/ww/wangnong/repository/AlertRepository.java` | 告警仓库 |
| `src/main/java/ww/wangnong/service/SensorService.java` | 传感器/设备统计服务接口 |
| `src/main/java/ww/wangnong/mock/MockSensorService.java` | 课程版模拟传感器数据实现 |
| `src/main/java/ww/wangnong/service/AlertService.java` | 告警服务接口 |
| `src/main/java/ww/wangnong/service/impl/DatabaseAlertService.java` | 告警服务实现 |
| `src/main/java/ww/wangnong/web/SensorController.java` | 传感器/设备统计 REST API |
| `src/main/java/ww/wangnong/web/AlertController.java` | 告警 REST API |
| `src/test/java/ww/wangnong/dto/ApiResponseTest.java` | 响应 DTO 测试 |
| `src/test/java/ww/wangnong/exception/BizExceptionTest.java` | 业务异常测试 |
| `src/test/java/ww/wangnong/exception/GlobalExceptionHandlerTest.java` | 全局异常测试 |
| `src/test/java/ww/wangnong/mock/MockSensorServiceTest.java` | 模拟传感器服务测试 |
| `src/test/java/ww/wangnong/repository/AlertRepositoryTest.java` | 告警仓库测试 |
| `src/test/java/ww/wangnong/service/impl/DatabaseAlertServiceTest.java` | 告警服务测试 |
| `src/test/java/ww/wangnong/web/SensorControllerTest.java` | 传感器 Controller 测试 |
| `src/test/java/ww/wangnong/web/AlertControllerTest.java` | 告警 Controller 测试 |
| `src/test/java/ww/wangnong/e2e/AlertE2eTest.java` | 告警 E2E 测试 |

## 5 号：定时任务、后台管理、操作日志和最终文档

负责任务 CRUD、管理员功能、API 文档、README、部署说明和五人分工文档。

| 文件 | 职责 |
|------|------|
| `.github/copilot-instructions.md` | GitHub Copilot 项目约束 |
| `.github/instructions/database.instructions.md` | 数据库开发约束 |
| `.github/instructions/java.instructions.md` | Java 开发约束 |
| `CHANGELOG.md` | 变更日志 |
| `README.md` | 项目说明 |
| `docs/api-endpoints.md` | API 文档 |
| `docs/course-team-split.md` | 五人分工文档 |
| `docs/deployment.md` | 部署说明 |
| `docs/frontend-requirements.md` | 前端需求范围文档 |
| `docs/frontend-page-flow.md` | 前端页面流程文档 |
| `docs/frontend-api-mapping.md` | 前端页面与后端接口映射文档 |
| `docs/frontend-state-and-auth.md` | 前端状态与认证约定文档 |
| `docs/frontend-env-and-run.md` | 前端环境与运行约定文档 |
| `src/main/java/ww/wangnong/entity/ScheduledTask.java` | 定时任务实体 |
| `src/main/java/ww/wangnong/repository/ScheduledTaskRepository.java` | 定时任务仓库 |
| `src/main/java/ww/wangnong/service/ScheduledTaskService.java` | 定时任务服务接口 |
| `src/main/java/ww/wangnong/service/impl/DatabaseScheduledTaskService.java` | 定时任务服务实现 |
| `src/main/java/ww/wangnong/service/AdminService.java` | 后台管理服务接口 |
| `src/main/java/ww/wangnong/service/impl/DatabaseAdminService.java` | 后台管理服务实现 |
| `src/main/java/ww/wangnong/web/ScheduledTaskController.java` | 定时任务 REST API |
| `src/main/java/ww/wangnong/web/AdminController.java` | 后台管理 REST API |
| `src/test/java/ww/wangnong/repository/ScheduledTaskRepositoryTest.java` | 定时任务仓库测试 |
| `src/test/java/ww/wangnong/service/impl/DatabaseScheduledTaskServiceTest.java` | 定时任务服务测试 |
| `src/test/java/ww/wangnong/service/impl/DatabaseAdminServiceTest.java` | 后台管理服务测试 |
| `src/test/java/ww/wangnong/web/ScheduledTaskControllerTest.java` | 定时任务 Controller 测试 |
| `src/test/java/ww/wangnong/web/AdminControllerTest.java` | 后台管理 Controller 测试 |
| `src/test/java/ww/wangnong/e2e/ScheduledTaskE2eTest.java` | 定时任务 E2E 测试 |
