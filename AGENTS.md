# mock-server (OSS) — agent brief

Use this file first. Scope: **self-hosted OpenAPI mock** — engine (pure Java), OpenAPI validation helpers, Spring Boot **standalone** + Next.js UI.

## Task → where to look

| Task | Start here | Typical touch |
|------|------------|---------------|
| Mock selection / variants / logs | `engine/src/main/java/io/spec0/mockserver/engine/` | `DefaultMockServerService`, `MockServerPersistencePort`, domain models |
| OpenAPI parse / schema validation | `openapi-validation/src/main/java/io/spec0/mockserver/openapi/validation/` | `DefaultMockOpenApiValidator`, `OpenApiSpecParser`, `SchemaValidationMode` |
| REST API / JPA / Flyway | `standalone/src/main/java/io/spec0/mockserver/` | `controller/`, `service/`, `domain/`, `repository/` |
| Static contract / Swagger | Repo root `openapi.yaml` | Keep in sync with controllers |
| Next.js UI | `ui/` | `basePath: /ui`; built into JAR via standalone build |
| MCP (standalone) | `standalone/.../standalone/mcp/` | Tools + config |
| Docker / local run | `docker-compose.yml`, root `README.md` | Env: `SPRING_DATASOURCE_URL`, `SERVER_PORT` |
| Docker REST E2E | `e2e-tests/`, `scripts/e2e-against-docker.sh` | Requires `-Dmock.server.baseUrl` (see `Makefile` `e2e-docker`) |

## Maven modules

| Module | Artifact (group `io.spec0`) | Role |
|--------|-----------------------------|------|
| `engine` | `mock-server-engine` | Framework-free core: services, ports, models, mockgen, dispatch |
| `openapi-validation` | `mock-server-openapi-validation` | Validation + parsed spec cache SPI |
| `standalone` | `mock-server-standalone` | Spring Boot app: JPA, Flyway, REST, UI static hosting |
| `e2e-tests` | `mock-server-e2e-tests` | Live HTTP tests against a running instance (not run in default `mvn test`) |

**Note:** `ui/` exists at repo root but is **not** always listed as a Maven reactor module; builds still expect `cd ui && npm ci && npm run build` before packaging standalone (see root README).

## Packages (Java)

| Package | Role |
|---------|------|
| `io.spec0.mockserver.engine.*` | Core logic (engine module) |
| `io.spec0.mockserver.openapi.validation.*` | OpenAPI validation (openapi-validation module) |
| `io.spec0.mockserver.adapter`, `controller`, `service`, `domain`, `repository`, `config`, `dto` | Standalone Spring layer (adapters implement engine ports) |

## HTTP API (standalone)

Contract: **`openapi.yaml`**. Live: **`/swagger-ui.html`** when running.

Common prefixes (see `standalone/.../controller/*`):

- **`/mock-server/specs`** — register/list OpenAPI specs
- **`/mock-server/servers`** — create/list mock servers
- **`/mock-server/servers/{mockServerId}/...`** — variants, config, logs, etc.
- **`/mock/{mockServerId}/...`** — **runtime** mock interception (pass-through to matched operation)

## Database

- **Schema**: **`mock_server`** (PostgreSQL-compatible DDL in migrations).
- **Flyway**: custom bean `mockServerFlyway` — locations **`classpath:db/mock-server`**, history table **`flyway_mock_server_history`**, **`spring.flyway.enabled=false`** on default app Flyway (see `MockServerConfiguration.java`).
- **Scripts**: `standalone/src/main/resources/db/mock-server/`
  - `V1__create_mock_server_schema.sql` — core tables
  - `V2__add_mcp_and_cel.sql`
  - `V3__schema_validation_mode.sql`

### Core tables (V1; verify in SQL + `domain/*Entity.java`)

`api_specs`, `mock_server_operations`, `mock_servers`, `mock_server_configs`, `mock_response_variants`, `mock_operation_configs`, `mock_request_logs`, … (full list in V1).

## Tests

```bash
./mvnw verify -Dskip.ui.copy=true     # from repo root — full verify, skip copying pre-built UI into JAR
mvn test                            # all modules
mvn -pl standalone test             # integration / web tests
mvn -pl engine test                 # pure engine tests
```

Key ITs live under `standalone/src/test/java/` (e.g. smoke, schema validation, static UI). Many use **`spring.flyway.enabled=false`** with test-specific setup — read the test class before changing Flyway.

## Formatting

`mvn spotless:apply` (Google Java Format) from repo root.

## Relation to spec0-platform

The product backend (`spec0-platform/apps/platform-backend-service`) consumes **`mock-server-engine`** and **`mock-server-standalone`** as libraries (with exclusions). **Behavioral fixes** often belong in **this OSS repo** first, then version bumps in platform. Platform-only orchestration stays in `spec0-platform`.

## Do not bulk-read

`ui/node_modules/`, `ui/out/`, `**/target/`, large `openapi.yaml` unless changing the contract — use Swagger or focused sections.
