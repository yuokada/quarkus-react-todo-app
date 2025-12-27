# Repository Guidelines

This document is a guide for contributors working in this repository. It lists concise, practical procedures.

## Project Structure
- Backend (Java/Quarkus): `src/main/java/io/github/yuokada/practice/`
- Resources/Configuration: `src/main/resources/` (`application.properties`, Redis load scripts, OpenAPI output location)
- Frontend (React/Vite): `src/main/webui/`
- Tests: `src/test/java/...`
- CI: `.github/workflows/maven.yml`
- Key files: `pom.xml`, `README.md`

## Build, Test, Development
- Dev mode (hot reload/Quinoa integration): `./mvnw compile quarkus:dev`
- Build JAR: `./mvnw package`
- Native build: `./mvnw package -Pnative`
- Test: `./mvnw test`
- Frontend only: `cd src/main/webui && npm run dev|build|preview`
Note: During dev, Quarkus proxies Vite (default 5173). OpenAPI output goes to `openapi-definition/`.

## Coding Conventions and Naming
- Java: 4-space indent, use `record` for DTOs (example: `TodoTask`). Package name is `io.github.yuokada.practice`.
- REST: Base paths `/api/todos`, `/api/async/todos`, `/increments`. Default to `@Produces(MediaType.APPLICATION_JSON)`.
- Lint/Format: Frontend uses Biome (`npm run biome:lint`, `npm run biome:format`). YAML/JSON are handled by pre-commit (`yamlfmt`, `check-yaml`, `check-json`).

## Testing Strategy
- Framework: Quarkus JUnit5 + RestAssured (example: `TodoAsyncResourceTest`).
- Execution: `./mvnw test`. Choose Redis DevServices or local `redis://localhost:6379/0` as needed.
- Naming: Test classes end with `*Test.java`. Prefer API tests that approach end-to-end coverage.

## Commit / PR
- Commits: Short phrase conveying intent + main point (example: "fix: async 更新404の扱いを修正").
- PR requirements: Overview/background, list of changes, verification steps (commands/screenshots), related issue links, impacted areas (API/configuration).

## Security / Configuration Tips
- Redis: Comment out `quarkus.redis.hosts` when using DevServices. For local use, set `%dev.quarkus.redis.hosts=redis://localhost:6379/0`.
- Node/JDK: Follow versions in `.tool-versions`; CI uses the same.
- Containers: Use Jib (`quarkus-container-image-jib`) to build images.
