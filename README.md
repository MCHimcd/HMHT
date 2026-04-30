# HMHT Course Backend

HMHT 课程版是鸿蒙慧田智慧农业系统的精简 Spring Boot 后端。项目只保留一个运行形态：Java 后端 + MySQL + 固定模拟节点数据，不包含 Python、硬件、MQTT、InfluxDB 或多运行模式配置。

## Tech Stack

| Area | Technology |
|------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle Wrapper 9.3.1 |
| Database | MySQL 8.x |
| Migration | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + BCrypt + business token |

## Project Structure

```text
src/main/java/ww/wangnong/
├── HmhtApplication.java
├── web/          # REST controllers
├── service/      # service contracts and database implementations
├── entity/       # JPA entities
├── repository/   # Spring Data repositories
├── mock/         # generated sensor history/statistics
├── config/       # security and course seed data
├── dto/          # API response DTOs
└── exception/    # business exception handling
```

## Course Data

Startup seeding is idempotent and creates:

| Item | Value |
|------|-------|
| Admin account | `13800000001` / `admin123` |
| Demo user | `13800000000` / `123456` |
| Node | `SIM_Node_01` / `课程模拟温室节点` |
| Sensor types | `temperature`, `humidity`, `light`, `soilHumidity` |
| Equipment types | `water_pump`, `led_light`, `fan` |

The sensor and equipment dictionaries are fixed for the course edition. Sensor history and equipment statistics are generated in memory for demo use; they are not read from real devices or a time-series database.

## Configuration

Main settings live in `src/main/resources/application.properties`.

| Property / Env | Default | Purpose |
|----------------|---------|---------|
| `server.port` | `8080` | HTTP port |
| `server.servlet.context-path` | `/api` | API base path |
| `HMHT_DB_URL` | `jdbc:mysql://localhost:3306/hmht?...` | MySQL JDBC URL |
| `HMHT_DB_USER` | `root` | MySQL user |
| `HMHT_DB_PASSWORD` | `123456` | MySQL password |
| `hmht.course.edition` | `true` | Course edition marker |

There is no runtime mode switch. The application always starts as the course edition.

## Build And Run

```bash
./gradlew bootRun
```

Compile and test:

```bash
./gradlew clean bootJar
./gradlew test
```

Expected artifact:

```text
build/libs/HMHT-0.0.1-SNAPSHOT.jar
```

## Frontend

The course frontend lives in `frontend/` and uses Vite, Vue 3, TypeScript, Pinia, Vue Router, and ECharts.

```bash
cd frontend
npm install
npm run dev
```

Default local addresses:

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:5173` |
| Backend API | `http://localhost:8080/api` |

Frontend environment example:

```dotenv
VITE_API_BASE_URL=/api
```

The Vite dev server proxies `/api` to `http://127.0.0.1:8080` for local browser testing.

Build and test:

```bash
cd frontend
npm run build
npm run test
```

## API Surface

Base path: `/api`.

| Area | Prefix | Responsibility |
|------|--------|----------------|
| Auth | `/v1/auth` | verification code, register, login, profile, password, logout |
| Node | `/v1/nodes` | one demo node, fixed sensor/equipment metadata, thresholds |
| Sensor | `/v1/sensors`, `/v1/equipment` | generated history and equipment statistics |
| Alert | `/v1/alerts` | alert list and handling |
| Task | `/v1/tasks` | user scheduled-task CRUD and enable toggle |
| Admin | `/v1/admin` | user, role, node, and user-node authorization management |
| Logs | `/v1/admin/logs` | operation log query |
| Health | `/v1/health` | course edition health check |

Detailed endpoint documentation is maintained in [docs/api-endpoints.md](docs/api-endpoints.md).

## Data Model

| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | `users` | user accounts |
| `VerificationCode` | `verification_codes` | registration/password-reset codes |
| `Node` | `nodes` | the single simulated node |
| `SensorType` | `sensor_type` | fixed sensor dictionary |
| `EquipmentType` | `equipment_type` | fixed equipment dictionary |
| `NodeSensor` | `node_sensor` | node-sensor binding and thresholds |
| `NodeEquipment` | `node_equipment` | node-equipment binding |
| `UserNodeAccess` | `user_node_access` | user-node authorization |
| `Alert` | `alerts` | alert records |
| `ScheduledTask` | `scheduled_tasks` | scheduled-task definitions |
| `OperationLog` | `operation_logs` | audit logs |

## Five-Person Course Split

| Person | Responsibility |
|--------|----------------|
| 1 | Java 25 build, MySQL/Flyway schema, seed data, health endpoint |
| 2 | Authentication and profile/password APIs |
| 3 | Node, fixed sensor/equipment metadata, threshold updates |
| 4 | Generated sensor history, equipment statistics, alert query/handling |
| 5 | Scheduled tasks, admin APIs, operation logs, final integration docs |

## Documentation

| File | Purpose |
|------|---------|
| [docs/api-endpoints.md](docs/api-endpoints.md) | Course edition API reference |
| [docs/course-team-split.md](docs/course-team-split.md) | Five-person file ownership split |
| [docs/deployment.md](docs/deployment.md) | Local deployment notes |
| [docs/frontend-requirements.md](docs/frontend-requirements.md) | Frontend scope and acceptance criteria |
| [docs/frontend-page-flow.md](docs/frontend-page-flow.md) | Frontend routes, navigation, and page states |
| [docs/frontend-api-mapping.md](docs/frontend-api-mapping.md) | Page-to-API mapping for frontend integration |
| [docs/frontend-state-and-auth.md](docs/frontend-state-and-auth.md) | Frontend token, state, and error handling rules |
| [docs/frontend-env-and-run.md](docs/frontend-env-and-run.md) | Frontend environment, local run, and deployment notes |
| [CHANGELOG.md](CHANGELOG.md) | Change history |
