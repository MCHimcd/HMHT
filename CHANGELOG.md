# Changelog

This file tracks HMHT project changes from this task onward.

Tracking starts on `2026-04-14`. Earlier project history is not backfilled here.

## 2026-05-03

- Fixed frontend alert handling by normalizing snake_case and camelCase alert fields, refreshed console/dashboard/realtime data every 2 seconds, added a working user menu, corrected alert counts, and restored the dashboard node-management icon contrast.
- Added simulated low-temperature alert generation when the 24h air-temperature stream drops below `5C`, with a cooldown and updated alert-count semantics/docs.
- Changed simulated sensor histories to rolling cached series so chart refreshes append only the newest point instead of regenerating previously displayed data.

## 2026-05-02

- Replaced the legacy equipment-statistics fallback node with the course node `SIM_Node_01` and updated the related controller test and API docs.
- Reorganized AI instruction documents with English section titles, repository-relative links, clearer priority rules, and reduced duplicated project constraints.
- Refreshed README and general docs to match the current course edition, Vue 3 frontend implementation, deployment workflow, route map, API request shapes, and frontend run scripts.
- Corrected documentation for the current datasource fallback password and the alert/equipment request payload formats used by the implementation.

## 2026-04-25

- Added the Spring Boot 4 Flyway auto-configuration module so database migrations run before JPA schema validation during backend startup.
- Reworked the authenticated frontend shell and node detail page to match the green-white smart agriculture dashboard concept with dense sidebar navigation, top status bar, KPI cards, trend chart, device statistics, alerts, and task tables.
- Switched local frontend API calls to the Vite `/api` proxy by default for reliable browser-based backend integration.
- Implemented the Vue 3 course frontend under `frontend/` with authentication, dashboard, node details, sensor charts, equipment stats, alerts, scheduled tasks, profile, admin management, operation logs, tests, and green-white smart agriculture styling.
- Updated frontend API mapping and run documentation to match the implemented request shapes and verification commands.
- Added ignore rules for macOS metadata, frontend local dependencies, and Vite build output.
- Reduced the frontend chart bundle by registering only the ECharts modules used by the course pages.
- Added frontend preparation docs for requirements, page flow, API mapping, state/auth rules, and local environment/run conventions.
- Updated README and course team ownership docs to reference the new frontend documentation.
- Switched the recommended frontend stack in documentation from React to Vue 3 with Pinia, Vue Router, and ECharts.

## 2026-04-23

- Cut HMHT down to a Java 25 course edition with a single fixed runtime shape.
- Removed MQTT, Python, hardware, InfluxDB, device capability, chat, traceability, camera, and infra debug surfaces.
- Rebuilt Flyway as `V1__course_schema.sql` for the retained MySQL business tables.
- Added idempotent course seed data for one simulated node `SIM_Node_01`, fixed sensor types, and fixed equipment types.
- Simplified auth by removing avatar upload endpoints and file storage configuration.
- Updated tests, README, API docs, deployment notes, and project instructions for the course edition.
- Added `docs/course-team-split.md` with file-level ownership for the five-person course split.
- Rebalanced `docs/course-team-split.md` around Java source line counts for the five-person split.

## 2026-04-14

- Rewrote `README.md` fully in English.
- Restructured the README into a spec-style documentation layout with consistent navigation and references.
- Added this `CHANGELOG.md` to start ongoing project change tracking.
- Updated `AGENTS.md` and `.github/copilot-instructions.md` to require appending `CHANGELOG.md` after each modification.
