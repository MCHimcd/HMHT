# Changelog

This file tracks HMHT project changes from this task onward.

Tracking starts on `2026-04-14`. Earlier project history is not backfilled here.

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
