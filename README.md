# HMHT Backend

Spring Boot backend for the Wangnong smart agriculture system. HMHT provides REST APIs, MQTT bridge capabilities, persistence, and runtime modes for the OpenHarmony frontend, the `agri` Python subsystem, and hardware integrations.

**Contents**: [Overview](#overview) · [Changelog](#changelog) · [Tech Stack](#tech-stack) · [Project Structure](#project-structure) · [Runtime Modes](#runtime-modes) · [Build Run Verification](#build-run-verification) · [API Surface](#api-surface) · [Data Model](#data-model) · [Services](#services) · [MQTT and Integration](#mqtt-and-integration) · [Security and Configuration](#security-and-configuration) · [Documentation](#documentation)

## Overview

HMHT is the backend service layer of the Wangnong platform. It supports:

- REST APIs for authentication, node management, device control, alerts, scheduling, traceability, and administration
- MQTT bridging between the app, Python `agri`, and hardware devices
- relational persistence through Spring Data JPA and Flyway
- optional InfluxDB-based telemetry storage
- multiple runtime modes for mock, bridge, hardware, and full-chain operation

The backend is designed to keep the application-facing API stable while allowing different data sources and routing strategies behind the same service contracts.

## Changelog

Project history from this task onward is tracked in [CHANGELOG.md](./CHANGELOG.md).

## Tech Stack

| Area | Technology | Version |
|------|------------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.5 |
| Build | Gradle Wrapper | — |
| Relational DB | MySQL | 8.x |
| Migration | Flyway | — |
| ORM | Spring Data JPA / Hibernate | — |
| Security | Spring Security + OAuth2 Resource Server | — |
| MQTT | Eclipse Paho Client | 1.2.5 |
| Time-series DB | InfluxDB 3 (`influxdb3-java`) | 1.0.0 |
| CI/CD | GitHub Actions | — |

## Project Structure

```text
HMHT/
├── src/main/java/ww/wangnong/
│   ├── HmhtApplication.java
│   ├── web/                    # REST controllers
│   ├── service/                # Service interfaces
│   │   └── impl/               # Default database-backed implementations
│   ├── entity/                 # JPA entities
│   ├── dto/                    # DTOs
│   ├── repository/             # Spring Data repositories
│   ├── mqtt/                   # MQTT client layer
│   ├── bridge/                 # MQTT/Python/hardware bridge logic
│   ├── influx/                 # InfluxDB integration
│   ├── mock/                   # Mock data and simulators
│   ├── config/                 # Spring configuration
│   │   └── condition/          # Runtime mode condition annotations
│   └── exception/              # Global exception handling
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/           # Flyway migrations
├── src/test/java/ww/wangnong/  # test suites
├── scripts/
│   ├── start.ps1
│   └── tests/
├── docs/
│   ├── api-endpoints.md
│   ├── backend-hardware-protocol.md
│   ├── hardware-team-action-guide.md
│   └── deployment.md
└── .github/
```

## Runtime Modes

Switch modes with `hmht.run.mode` or `HMHT_RUN_MODE`.

| Mode | Purpose | Data Path |
|------|---------|-----------|
| `mock` | Local demo mode with seeded data | mock lifecycle + InfluxDB demo history |
| `bridge` | App to Java to Python chain | Python `agri` -> MQTT -> bridge -> app |
| `hardware` | App to Java to hardware chain | hardware MQTT -> bridge -> app |
| `full` | Full end-to-end routing | Python + hardware -> bridge -> app |

Runtime wiring is controlled by custom condition annotations under `config/condition/`:

- `@ConditionalOnMockMode`
- `@ConditionalOnBridgeMode`
- `@ConditionalOnPythonRoute`
- `@ConditionalOnHardwareRoute`
- `@ConditionalOnNoPythonRoute`

Operational notes:

- A default admin account `13800000001` is ensured at startup.
- If a legacy plaintext default password `admin123` is detected, it is upgraded to a BCrypt hash without overwriting a user-changed password.
- In `full` mode, hardware telemetry takes precedence over Python telemetry for the same `deviceId` once real hardware data has been received.
- In `mock` mode, `SIM_Node_01` demo telemetry, alerts, and video fallback are primarily generated on the Wangnong frontend when `HMHT_MOCK_SIM_NODE_FRONTEND_DRIVEN=true`.

### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `HMHT_RUN_MODE` | `mock` | runtime mode |
| `HMHT_MOCK_SEED_ENABLED` | `true` | enable mock seed data |
| `HMHT_MOCK_SIM_NODE_FRONTEND_DRIVEN` | `true` | let the frontend drive `SIM_Node_01` demo data |
| `HMHT_AGRI_CHAT_URL` | `http://localhost:5000` | Python chat service endpoint |
| `HMHT_INFLUXDB_ENABLED` | `true` | enable InfluxDB writes |
| `HMHT_INFLUXDB_TOKEN` | empty | InfluxDB access token |
| `HMHT_COMMAND_RETRY_CHECK_INTERVAL_MS` | `5000` | ACK timeout scan interval |
| `HMHT_COMMAND_RETRY_MAX_RETRIES` | `3` | maximum command retries |
| `HMHT_COMMAND_RETRY_DEFAULT_TIMEOUT_MS` | `5000` | default command timeout |

By default, Hibernate SQL logging is disabled with `spring.jpa.show-sql=false`.

## Build Run Verification

### Local Development

Mock mode:

```bash
./gradlew bootRun
```

Full-chain startup:

```powershell
.\scripts\start.ps1 -Mode full
```

The startup script supports `-Mode`, `-PythonDir`, `-JavaDir`, `-AgriChatPort`, `-InfluxDir`, and `-InfluxToken`. The Windows workflow requires `wt.exe`.

### Script-Level Verification

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\tests\start.ps1.test.ps1
```

### Compile and Test

```bash
./gradlew clean bootJar
./gradlew test
```

Expected outputs:

- JAR artifact: `build/libs/HMHT-0.0.1-SNAPSHOT.jar`
- test report: `build/reports/tests/test/index.html`

### Test Inventory

| Layer | Test Style | Count |
|------|------------|-------|
| DTO / exception | pure JUnit `@Test` | 3 |
| Bridge / MQTT / Influx | Mockito extension | 11 |
| Repository | `@DataJpaTest` | 11 |
| Service | Mockito extension | 6 |
| Controller | `@WebMvcTest` | 12 |
| Mock / simulator | Mockito extension | 5 |
| Bootstrap / foundation | `@SpringBootTest` | 4 |
| End-to-end | `@SpringBootTest` | 3 |

Total documented coverage: **55 test classes**.

## API Surface

Base path: `/api` through `server.servlet.context-path=/api`

| Controller | Route Prefix | Responsibility | Endpoints |
|------------|--------------|----------------|-----------|
| `AuthController` | `/v1/auth` | login, registration, verification, password, profile, avatar | 10 |
| `NodeController` | `/v1/nodes` | node list, detail, metadata, thresholds | 5 |
| `DeviceController` | `/v1/devices` | device capability snapshot and shadow | 2 |
| `SensorController` | `/v1/sensors`, `/v1/equipment` | sensor history and equipment statistics | 2 |
| `AlertController` | `/v1/alerts` | alert query and handling | 2 |
| `ScheduledTaskController` | `/v1/tasks` | scheduled task CRUD | 5 |
| `ChatController` | `/v1/agri-qa` | agriculture AI chat | 1 |
| `ExtraController` | `/v1/traceability`, `/v1/cameras` | traceability scan and camera APIs | 4 |
| `AdminController` | `/v1/admin` | node, user, and permission administration | 7 |
| `OperationLogController` | `/v1/admin/logs` | operation log queries | 1 |
| `HealthController` | `/v1` | frontend availability check | 1 |
| `InfraController` | `/infra` | health and manual MQTT publish | 2 |

Detailed endpoint documentation is maintained in [docs/api-endpoints.md](docs/api-endpoints.md).

## Data Model

All entities live under `ww.wangnong.entity` and use Lombok `@Getter @Setter`.

### Core Business Tables

| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | `users` | user accounts |
| `Node` | `nodes` | agriculture nodes |
| `SensorType` | `sensor_type` | sensor type definitions |
| `NodeSensor` | `node_sensor` | node-sensor binding and thresholds |
| `EquipmentType` | `equipment_type` | equipment type definitions |
| `NodeEquipment` | `node_equipment` | node-equipment binding |
| `Alert` | `alerts` | alert records |
| `CommandLog` | `command_logs` | command logs |
| `ChatMessage` | `chat_messages` | chat history |
| `ScheduledTask` | `scheduled_tasks` | scheduled tasks |
| `OperationLog` | `operation_logs` | audit logs |
| `VerificationCode` | `verification_codes` | verification codes |
| `UserNodeAccess` | `user_node_access` | user-node access control |

### V2 Control Plane Tables

| Entity | Table | Purpose |
|--------|-------|---------|
| `CommandRequest` | `command_request` | command lifecycle main table |
| `CommandAck` | `command_ack` | command acknowledgement records |
| `CommandRetryLog` | `command_retry_log` | retry tracking |
| `DeviceProfile` | `device_profile` | device profile |
| `DeviceCapability` | `device_capability` | capability snapshot |
| `CapabilitySensor` | `capability_sensor` | sensor capability details |
| `CapabilityEquipment` | `capability_equipment` | equipment capability details |
| `DeviceShadow` | `device_shadow` | device shadow state |
| `IngestAudit` | `ingest_audit` | message ingest audit |
| `IngestDeadletter` | `ingest_deadletter` | dead-letter storage |

## Services

Service interfaces under `ww.wangnong.service` are backed by different implementations depending on runtime mode.

| Interface | Default Implementation | Mode-Specific Implementation | Purpose |
|-----------|------------------------|------------------------------|---------|
| `AuthService` | `DatabaseAuthService` | — | authentication and user management |
| `NodeService` | `DatabaseNodeService` | — | node metadata aggregation |
| `AlertService` | `DatabaseAlertService` | — | alert query and handling |
| `SensorService` | — | `MockSensorService` in `mock`, `InfluxSensorService` in bridge-plus modes | sensor history |
| `DeviceService` | `DatabaseDeviceService` | — | capability and shadow access |
| `ChatService` | — | `MockChatService` in `mock`, `AgriChatService` in bridge-plus modes | AI chat |
| `ExtraService` | — | `MockExtraService` in `mock` | traceability and camera data |
| `AdminService` | `DatabaseAdminService` | — | administrative operations |
| `ScheduledTaskService` | `DatabaseScheduledTaskService` | — | scheduled task management |
| `OperationLogService` | direct implementation | — | operation logging |

Supporting components include `MockDataLifecycle`, `MockMqttSimulator`, `MockAlertGenerator`, `MockTaskExecutor`, `TelemetryTranslator`, `InfluxTelemetryWriter`, `ProtocolPersistenceService`, `CommandAckTracker`, `PahoMqttService`, and `MqttSubscriptionRegistry`.

## MQTT and Integration

Protocol details are documented in [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md).

### Hardware Topics

| Direction | Topic | Purpose |
|-----------|-------|---------|
| Device -> HMHT | `/hmht/device/{deviceId}/telemetry` | telemetry upload |
| HMHT -> Device | `/hmht/device/{deviceId}/command` | control command |
| Device -> HMHT | `/hmht/device/{deviceId}/ack` | command acknowledgement |
| HMHT -> App | `/wangnong_2026/node/{nodeId}/ack` | acknowledgement forwarding |

### Legacy `agri` Topics

| Direction | Topic | Purpose |
|-----------|-------|---------|
| Python -> HMHT | `/agri/device/{deviceId}/telemetry` | Python telemetry |
| Python -> HMHT | `/agri/ai_vision/alert` | AI vision alert |
| HMHT -> App | `/wangnong_2026/node/{nodeId}/telemetry` | translated telemetry |
| HMHT -> App | `/wangnong_2026/node/{nodeId}/alert` | translated alert |
| App -> Python | `.../command` -> `/agri/device/{deviceId}/command` | command forwarding |

### Bridge Behavior

- command handling and telemetry handling use separate processing paths
- telemetry processing is ordered to avoid stale state overwriting fresh state
- legacy `agri` topics remain compatible so app subscriptions stay unchanged

### Collaboration with `agri`

| Python Service | File | HMHT Interaction |
|----------------|------|------------------|
| backend main process | `backend_main_debug.py` | MQTT bridge |
| web service | `web_server_debug.py` | HTTP proxy via `AgriChatService` |
| mock device | `mock_device.py` | MQTT bridge |

## Security and Configuration

- `SecurityConfig` currently permits all requests
- CORS is open and CSRF is disabled
- controllers extract `Authorization: Bearer {token}` and business validation happens in the service layer
- structured configuration uses `@ConfigurationProperties`, including `MqttProperties` and `InfluxDbProperties`
- sensitive values are expected through environment variables

### Response Contract

```json
{ "code": 200, "message": "success", "data": {} }
```

Business errors are raised through `BizException(code, message)` and converted by `GlobalExceptionHandler` into `ApiResponse.error()`.

### Database Migration

- migration directory: `src/main/resources/db/migration/`
- documented current control-plane version: V2
- naming format: `V{N}__{description}.sql`
- strategy: Flyway creates baseline schema and Hibernate `update` applies supplemental changes

## Documentation

| Document | Purpose |
|----------|---------|
| [CHANGELOG.md](./CHANGELOG.md) | ongoing project history |
| [docs/api-endpoints.md](docs/api-endpoints.md) | detailed REST endpoints |
| [docs/backend-hardware-protocol.md](docs/backend-hardware-protocol.md) | backend and hardware protocol |
| [docs/hardware-team-action-guide.md](docs/hardware-team-action-guide.md) | hardware integration guide |
| [docs/deployment.md](docs/deployment.md) | deployment workflow |
