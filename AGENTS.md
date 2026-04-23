# HMHT Instructions For Codex

Apply these rules for all files under `HMHT/`.

Read first:
- [.github/copilot-instructions.md](/d:/projects/ww/HMHT/.github/copilot-instructions.md)
- [.github/instructions/java.instructions.md](/d:/projects/ww/HMHT/.github/instructions/java.instructions.md)
- [.github/instructions/database.instructions.md](/d:/projects/ww/HMHT/.github/instructions/database.instructions.md) for entity, repository, and migration work

Non-negotiable rules:
- Clarify unclear requirements before implementation.
- Update tests first or together with the implementation.
- Sync `README.md` and `docs/api-endpoints.md` when controllers, services, entities, env vars, migrations, or the fixed course data change.
- After each modification, append the change to `CHANGELOG.md`.
- Final verification for Java changes: `./gradlew clean bootJar` and `./gradlew test`.

Key coding constraints:
- Use constructor injection, never field `@Autowired`.
- Do not add runtime modes, MQTT, Python, hardware, or InfluxDB integration to the course edition.
- Prefer Java `record` for DTOs.
- Use `@Getter @Setter` for entities, not `@Data`.
- Use `ApiResponse<T>` for responses and `BizException` for business errors.
