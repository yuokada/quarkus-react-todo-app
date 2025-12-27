# Quarkus React Todo App

This project combines a Quarkus REST backend with a Vite/React Todo UI using the Quarkiverse Quinoa extension. Redis is used as a lightweight data store backing the Todo API.

## Architecture at a Glance
- **Backend**: Quarkus 3 + RESTEasy Reactive + Quarkus Redis Client. All resources live under `src/main/java/io/github/yuokada/practice`.
- **Frontend**: React (Vite) with the main logic in `src/main/webui/src/App.jsx`.
- **Storage**: Redis (DevServices or `redis://localhost:6379/0`). Tasks are stored as the `TodoTask` record.
- **Quinoa**: Bridges Quarkus and the Vite app. In dev mode it proxies the Vite dev server; in prod it serves the built `dist/index.html` and assets.

## Common Commands
| Purpose | Command | Notes |
| --- | --- | --- |
| Backend + frontend live coding | `./mvnw compile quarkus:dev` | Launches Quarkus dev mode and Quinoa-managed Vite dev server at `http://localhost:8080`. Dev UI at `/q/dev`. |
| Backend tests | `./mvnw test` | Uses Redis DevServices or local Redis depending on config. |
| Package JAR | `./mvnw package` | Artifacts end up in `target/quarkus-app/`. |
| Native build | `./mvnw package -Pnative` | Add `-Dquarkus.native.container-build=true` to build via container. |
| Frontend dev only | `cd src/main/webui && npm run dev` | Runs Vite standalone (default port 5173). |
| Frontend build | `cd src/main/webui && npm run build` | Updates Quinoa’s source `dist/` folder. |
| Build container image (Jib) | `./mvnw package -Dquarkus.container-image.build=true` | Produces an OCI image via Jib; append `-Dquarkus.container-image.push=true` to push. |

## Quinoa Configuration
`src/main/resources/application.properties` contains:
```properties
quarkus.quinoa.dev-server-port=5173
quarkus.quinoa.build-dir=dist
quarkus.quinoa.index-page=index.html
quarkus.quinoa.enable-spa-routing=true
```
This mimics the setup recommended in the [Advanced Guides](https://docs.quarkiverse.io/quarkus-quinoa/dev/advanced-guides.html). Dev mode proxies Vite, while prod serves the generated `dist/index.html` and falls back to it for SPA routing.

## REST API Summary
Base URL: `http://localhost:8080/api/todos`

| Method | Path | Description |
| --- | --- | --- |
| `GET /` | Fetch all tasks |
| `GET /{id}` | Fetch a single task |
| `POST /` | Create a task (`{"title":"...", "completed":false}`) |
| `PUT /{id}` | Update title/completed |
| `DELETE /{id}` | Delete a task (404 when missing) |

Additional async endpoints (`/api/async/todos`) and increment examples are also available in the same package. OpenAPI definitions are emitted into `openapi-definition/`.

see also: http://localhost:8080/q/swagger-ui/

## Frontend Highlights
- `App.jsx` implements list rendering, creation form, completion toggles, and deletion, all backed by fetch calls to `/api/todos`.
- `App.css` provides a lightweight card-style layout with responsive tweaks.
- Error/loading states feed back to the UI via status messages.

## Redis Usage
- **DevServices**: Leaving `%dev.quarkus.redis.hosts` commented lets Quarkus start Redis automatically in dev/test.
- **Local Redis**: Set `%dev.quarkus.redis.hosts=redis://localhost:6379/0` to reuse an existing instance.
- Seed scripts (`users.redis`, `test-task.redis`) help bootstrap data when needed.

## Testing
- Tests live under `src/test/java/...` and rely on Quarkus JUnit5 + RestAssured.
- `%test.quarkus.redis.load-script=test-task.redis` loads fixtures for deterministic results.

## Container Image (Jib)
- The dependency `quarkus-container-image-jib` is included and configured via `application.properties`.
- Default image coordinates: `io.github.yuokada.practice/quarkus-react-todo-app:latest-jib`.
- Build locally: `./mvnw package -Dquarkus.container-image.build=true`
- Push to a registry: `./mvnw package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true`
- Override registry/name/tag as needed with `quarkus.container-image.*` properties or system properties (see [Quarkus container image guide](https://quarkus.io/guides/container-image)).

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): Build reactive REST services with Quarkus.
- Quinoa ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)): Develop, build, and serve SPA frontends alongside Quarkus services.

Live code the backend and frontend together with minimal configuration—Quinoa proxies the framework dev server during development and serves the generated assets in production.

## Provided Code

### Quinoa

This is a tiny webpack app to get started with Quinoa. It generates a quinoa.html page and a script.

[Related guide section...](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)

### React + Vite

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/README.md) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### Redis

- [Redis Extension Reference Guide - Quarkus](https://quarkus.io/guides/redis-reference)
- [Using the Redis Client - Quarkus](https://quarkus.io/guides/redis)
- [Dev Services for Redis - Quarkus](https://quarkus.io/guides/redis-dev-services)
- [valkey/valkey - Docker Image](https://hub.docker.com/r/valkey/valkey)

For more contributor-oriented details (naming, linting, etc.), see `AGENTS.md`.
