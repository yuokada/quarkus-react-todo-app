# Quarkus React Todo App

This project combines a Quarkus REST backend with a Vite/React Todo UI using the Quarkiverse Quinoa extension. Redis is used as a lightweight data store backing the Todo API.

## Architecture at a Glance
- **Backend**: Quarkus 3 + RESTEasy Reactive + Quarkus Redis Client. The Java code is organized into domain, application, infrastructure, and presentation packages under `src/main/java/io/github/yuokada/practice`.
- **AI agent**: Quarkus LangChain4j connects OpenAI or Gemini to typed Todo tools. The model can inspect and change tasks only through `TodoService`.
- **Frontend**: React (Vite) with the main logic in `src/main/webui/src/App.jsx`.
- **Storage**: Redis (DevServices or `redis://localhost:6379/0`). Tasks are stored as the `TodoTask` record.
- **Quinoa**: Bridges Quarkus and the Vite app. In dev mode it proxies the Vite dev server; in prod it serves the built `dist/index.html` and assets.

## Package Structure
The backend now follows a simple Clean Architecture-style package split.

- `io.github.yuokada.practice.domain.model`
  Holds business data types such as `TodoTask`, `Status`, and `Increment`. These classes do not depend on Quarkus or Redis APIs.
- `io.github.yuokada.practice.domain.repository`
  Defines repository interfaces used by the application layer, such as `TodoRepository`, `TodoAsyncRepository`, and `IncrementRepository`.
- `io.github.yuokada.practice.application.service`
  Contains use-case services like `TodoService`, `TodoAsyncService`, and `IncrementService`. These services depend on repository interfaces, not on Redis-specific implementations.
- `io.github.yuokada.practice.infrastructure.redis`
  Contains Redis-backed implementations such as `RedisTodoRepository`, `RedisTodoAsyncRepository`, and `RedisIncrementRepository`. Quarkus Redis datasource access is isolated here.
- `io.github.yuokada.practice.infrastructure.dynamodb`
  Contains DynamoDB-backed implementations such as `DynamoDbTodoRepository`, `DynamoDbTodoAsyncRepository`, `DynamoDbIncrementRepository`, and `DynamoDbClientProducer`. AWS SDK v2 access is isolated here.
- `io.github.yuokada.practice.presentation.rest`
  Contains JAX-RS resource interfaces and implementations such as `TodoResourceImpl`, `TodoAsyncResourceImpl`, and `IncrementResource`. This layer maps HTTP requests and responses to application services.

Dependency direction:
- `presentation` -> `application` -> `domain`
- `infrastructure` -> `domain`

In practice, this means REST resources do not talk to Redis directly, and application services do not import Quarkus Redis APIs.

## Common Commands
| Purpose | Command | Notes |
| --- | --- | --- |
| Backend + frontend live coding | `mise run dev` | Starts Valkey, then launches Quarkus dev mode and the Quinoa-managed Vite dev server at `http://localhost:8080`. Dev UI at `/q/dev`. |
| Backend tests | `./mvnw test` | Uses Redis DevServices or local Redis depending on config. |
| Package JAR | `./mvnw package` | Artifacts end up in `target/quarkus-app/`. |
| Native build | `./mvnw package -Pnative` | Add `-Dquarkus.native.container-build=true` to build via container. |
| Start local Valkey | `mise run valkey-up` | Exposes Valkey on `localhost:6379` for local dev. |
| Stop local Valkey | `mise run valkey-down` | Stops the Compose services without deleting the Valkey data volume. |
| Frontend lint/check | `cd src/main/webui && npm run biome:check` | Runs Biome without modifying files. |
| Frontend dev only | `cd src/main/webui && npm run dev` | Runs Vite standalone (default port 5173). |
| Frontend build | `cd src/main/webui && npm run build` | Updates Quinoa’s source `dist/` folder. |
| Build container image (Jib) | `./mvnw package -Dquarkus.container-image.build=true` | Produces an OCI image via Jib; append `-Dquarkus.container-image.push=true` to push. |

Toolchain versions are managed by [mise](https://mise.jdx.dev/) in `mise.toml`:
- Java: Temurin 25
- Node.js: 26

After installing mise, provision the project toolchain from the repository root:

```shell
mise install
mise exec -- java -version
mise exec -- node --version
```

mise derives `JAVA_HOME` from the configured Java installation, so activated shells and commands executed with `mise exec` use the selected Temurin JDK automatically.

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

Additional async endpoints (`/api/async/todos`) and increment examples are exposed from the presentation layer. OpenAPI definitions are emitted into `openapi-definition/`.

see also: http://localhost:8080/q/swagger-ui/

## AI Todo Agent

The application includes a chat-based Todo agent at `POST /api/agent`. It can list, inspect,
create, update, and delete Todo tasks by calling typed LangChain4j tools. Tool calls go through
`TodoService`, so the same agent works with Valkey in development and DynamoDB in production.

The browser UI displays the agent next to the Todo list. After an agent response, the list is
automatically refreshed. Conversation memory keeps the most recent 10 messages per `sessionId`
in application memory. It is cleared when the application restarts.

### OpenAI setup

Create an [OpenAI API key](https://platform.openai.com/api-keys) with API billing or credits
enabled, then export it in the shell that starts Quarkus. A ChatGPT subscription does not
automatically provide API usage.

```shell
export AI_PROVIDER=openai
export OPENAI_API_KEY='sk-...'
# Optional; defaults to gpt-4o-mini
export OPENAI_MODEL='gpt-4o-mini'
mise run dev
```

### Gemini setup

Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/app/apikey) and select
the Gemini provider:

```shell
export AI_PROVIDER=ai-gemini
export GEMINI_API_KEY='...'
# Optional; defaults to gemini-2.5-flash
export GEMINI_MODEL='gemini-2.5-flash'
mise run dev
```

Only one provider key is required. `AI_PROVIDER` accepts `openai` or `ai-gemini`; OpenAI is the
default. Provider, key, and model settings can also be supplied with the corresponding Quarkus
environment variables documented in `application.properties`.

Never put a real API key in the React source, `application.properties`, or `mise.toml`. Keep it in
the server process environment or a production secrets manager. The repository ignores `.env`,
but exporting secrets directly or using a dedicated secret manager is preferred.

### Using the agent

Open <http://localhost:8080> and enter a request in the **Todo assistant** panel, for example:

- `未完了のタスクを一覧にして`
- `「リリースノートを書く」という未完了タスクを追加して`
- `ID 4 のタスクを完了にして`
- `ID 7 を削除して` (the agent asks for explicit confirmation before deletion)

The endpoint can also be called directly. Reuse the same `sessionId` to continue a conversation:

```shell
curl -X POST http://localhost:8080/api/agent \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-user","message":"未完了のタスクを教えて"}'
```

Example response:

```json
{"message":"未完了のタスクは3件あります。..."}
```

Requests are limited to 2,000 characters and five sequential tool executions. Automated tests
disable both live model integrations and never call OpenAI or Gemini.

## Frontend Highlights
- `App.jsx` implements list rendering, creation form, completion toggles, and deletion, all backed by fetch calls to `/api/todos`.
- `App.css` provides a lightweight card-style layout with responsive tweaks.
- Error/loading states feed back to the UI via status messages.

## Backend Selection

The backend storage can be switched at runtime via `app.repository.type` in `application.properties`.

| Value | Backend |
| --- | --- |
| `redis` (dev/test default) | Valkey/Redis |
| `dynamodb` (production default) | DynamoDB |

Dev mode uses Valkey through Quarkus DevServices by default. DynamoDB remains available for
production and is covered by dedicated integration tests that start DynamoDB Local through
Testcontainers.

## Redis Usage
- **DevServices**: Leaving `%dev.quarkus.redis.hosts` commented lets Quarkus start Redis automatically in dev/test.
- **Local Redis**: Set `%dev.quarkus.redis.hosts=redis://localhost:6379/0` to reuse an existing instance.
- **Docker Compose**: `mise run dev` starts the Valkey container defined in [`compose.yaml`](compose.yaml), disables Redis DevServices for that process, and connects Quarkus to `redis://localhost:6379/0`. Use `mise run valkey-down` when finished.
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
- Quarkus LangChain4j ([guide](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)): Configure AI services, tools, and model providers.
- LangChain4j function calling ([guide](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html)): Expose typed application operations to a model.
- Quinoa ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)): Develop, build, and serve SPA frontends alongside Quarkus services.
- DynamoDB support plan: [`docs/dynamodb-support-plan.md`](docs/dynamodb-support-plan.md)
- Reactive return type notes: [`docs/reactive-return-types.md`](docs/reactive-return-types.md)
- Redis reactive tutorial: [`docs/redis-reactive-tutorial.md`](docs/redis-reactive-tutorial.md)

Live code the backend and frontend together with minimal configuration—Quinoa proxies the framework dev server during development and serves the generated assets in production.

## Provided Code

### Quinoa

This project uses Quinoa to wire a Vite/React frontend into the Quarkus application lifecycle.

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
