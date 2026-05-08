# HMHT Agent Instructions

Apply these rules to every file under this repository.

## Priority

1. Follow this file for repository entry rules.
2. Read [.github/copilot-instructions.md](.github/copilot-instructions.md) before planning or editing.
3. For Java changes, also read [.github/instructions/java.instructions.md](.github/instructions/java.instructions.md).
4. For entity, repository, or migration changes, also read [.github/instructions/database.instructions.md](.github/instructions/database.instructions.md).

No `agent.md` file is maintained in this repository; `AGENTS.md` is the agent entry point.

## Workflow

- Clarify requirements before implementation when scope, behavior, or ownership is ambiguous.
- Update tests first or together with implementation.
- Write documentation with English titles/headings and Chinese detailed content.
- Do not modify source code for documentation-only tasks.

## Project Boundaries

- HMHT is a course-edition Spring Boot backend with a Vue 3 frontend.
- Runtime shape is fixed: Java backend, MySQL, Flyway, fixed simulated course data, and generated demo sensor data.
- Do not add runtime modes, MQTT, Python, hardware integration, InfluxDB, device ACK loops, or external time-series data.

## Sync Rules

When controllers, services, entities, environment variables, migrations, fixed course data, or frontend request contracts change, keep these documents aligned:

- `README.md`
- `docs/api-endpoints.md`
- related `docs/*.md`

## Java Rules

- Use constructor injection; never use field `@Autowired`.
- Prefer Java `record` for DTOs.
- Use `@Getter @Setter` for entities, not `@Data`.
- Return `ApiResponse<T>` from controllers.
- Use `BizException` for business errors.

## Verification

For Java changes, run:

```bash
./gradlew clean bootJar
./gradlew test
```

For frontend changes, run the relevant `frontend/package.json` scripts, usually:

```bash
cd frontend
npm run build
npm run test
```
