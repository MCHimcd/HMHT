# Course Team Split

本分工面向 HMHT 课程版单体应用，包含 Spring Boot 后端和 Vue 3 前端。五个人在同一个仓库中协作，最终组合为一个可运行应用。运行形态固定为 Java 25 + MySQL + `SIM_Node_01` 模拟节点，不包含 MQTT、Python、硬件、InfluxDB 或多运行模式。

## Principles

- 每个文件只有一个主负责人，避免多人同时修改同一文件。
- 公共接口变更必须提前同步给相关负责人。
- Controller、Service、Entity、Repository、测试和文档必须一起维护。
- 前后端按同一业务域分组，尽量减少跨人改同一功能。
- 代码量以 `.java`、`.vue`、`.ts`、`.css` 为主做均衡。
- 每次合并前至少运行 `./gradlew test`；集成负责人最终运行 `./gradlew clean bootJar`。

## Person 1: Build, Bootstrap, Schema, Logs, And Frontend Shell

负责 Java 25 工程、课程版启动、固定种子数据、数据库迁移、健康检查、操作日志和前端外壳与样式基础。

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
| `frontend/index.html` | 前端 HTML 入口 |
| `frontend/package.json` | 前端脚本和依赖清单 |
| `frontend/tsconfig.json` | 前端 TypeScript 配置 |
| `frontend/vite.config.ts` | Vite 构建配置 |
| `frontend/src/main.ts` | 前端启动入口 |
| `frontend/src/App.vue` | 根组件 |
| `frontend/src/components/AppShell.vue` | 应用外壳、导航和整体布局 |
| `frontend/src/styles/main.css` | 全局样式 |
| `frontend/src/api/health.ts` | 健康检查请求封装 |
| `frontend/src/env.d.ts` | Vite 环境类型声明 |
| `frontend/src/utils/format.ts` | 通用格式化工具 |
| `frontend/src/test/client.test.ts` | 前端 API 客户端基础测试 |

## Person 2: Auth, Profile, And Access Entry

负责验证码、注册、登录、个人资料、密码、登出、业务 token 校验和前端认证入口。

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
| `frontend/src/api/auth.ts` | 认证接口请求封装 |
| `frontend/src/api/client.ts` | 前端统一 HTTP 客户端 |
| `frontend/src/router/index.ts` | 路由表和页面访问入口 |
| `frontend/src/stores/auth.ts` | 登录态与 token 状态管理 |
| `frontend/src/utils/domain.ts` | 认证和业务域名工具 |
| `frontend/src/components/AuthPanel.vue` | 登录/注册相关公共面板 |
| `frontend/src/pages/GatewayPage.vue` | 课程版访问入口页 |
| `frontend/src/pages/LoginPage.vue` | 登录页 |
| `frontend/src/pages/RegisterPage.vue` | 注册页 |
| `frontend/src/pages/ForgotPasswordPage.vue` | 忘记密码页 |
| `frontend/src/pages/ProfilePage.vue` | 个人资料页 |

## Person 3: Node, Sensor Metadata, Equipment Metadata, And Node Frontend

负责 `SIM_Node_01`、固定传感器字典、固定设备字典、节点授权、节点元数据、阈值更新和节点详情前端。

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
| `frontend/src/api/nodes.ts` | 节点接口请求封装 |
| `frontend/src/components/DataState.vue` | 数据状态展示组件 |
| `frontend/src/pages/NodeDetailPage.vue` | 节点详情页 |
| `frontend/src/pages/EquipmentTemplatePage.vue` | 固定设备模板页 |
| `frontend/src/pages/SensorHistoryPage.vue` | 节点侧传感器历史页 |
| `frontend/src/pages/StrategyPage.vue` | 阈值和策略配置页 |

## Person 4: Sensor Simulation, Equipment Stats, Alerts, Responses, And Data Frontend

负责传感器模拟数据、设备统计、告警列表、告警处理、统一响应、异常处理和数据看板前端。

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
| `frontend/src/api/alerts.ts` | 告警接口请求封装 |
| `frontend/src/api/sensors.ts` | 传感器与设备统计接口封装 |
| `frontend/src/components/ChartPanel.vue` | 数据图表组件 |
| `frontend/src/pages/DashboardPage.vue` | 数据总览页 |
| `frontend/src/pages/RealtimeDataPage.vue` | 实时数据页 |
| `frontend/src/pages/EquipmentStatsPage.vue` | 设备统计页 |
| `frontend/src/pages/DeviceControlPage.vue` | 设备控制页 |
| `frontend/src/pages/AlertsPage.vue` | 告警页 |
| `frontend/src/test/alerts.test.ts` | 告警前端测试 |

## Person 5: Tasks, Admin, Frontend Ops, And Documentation

负责任务 CRUD、管理员功能、前端运维管理页面、API 文档、README、部署说明和五人分工文档。

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
| `frontend/src/api/admin.ts` | 管理接口请求封装 |
| `frontend/src/api/tasks.ts` | 任务接口请求封装 |
| `frontend/src/api/types.ts` | 前端共享业务类型定义 |
| `frontend/src/pages/AdminPage.vue` | 管理后台页 |
| `frontend/src/pages/TasksPage.vue` | 任务管理页 |
| `frontend/src/pages/LogsPage.vue` | 操作日志页 |
| `frontend/src/test/console.test.ts` | 前端日志工具测试 |
| `frontend/src/test/tasks.test.ts` | 任务前端测试 |
| `frontend/src/utils/console.ts` | 前端日志输出工具 |
| `frontend/src/utils/tasks.ts` | 任务页面工具 |
