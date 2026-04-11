# spec0 Mock Server — Standalone

A self-hosted, open-source mock server for OpenAPI specs. Register a spec, get auto-generated
mock responses immediately. Persist custom variants across restarts. No cloud account required.

---

## Table of Contents

- [Quick start — without Docker](#quick-start--without-docker)
- [Quick start — with Docker](#quick-start--with-docker)
  - [macOS / Linux](#macos--linux)
  - [Windows (PowerShell)](#windows-powershell)
  - [Windows (Command Prompt)](#windows-command-prompt)
  - [WSL2](#wsl2)
- [Using with the UI](#using-with-the-ui)
  - [Run the standalone UI](#run-the-standalone-ui)
  - [What the UI covers](#what-the-ui-covers)
  - [PATCH endpoints added for UI operations](#patch-endpoints-added-for-ui-operations)
  - [Platform mode](#platform-mode-spec0-cloud--self-hosted-platform)
- [REST API reference](#rest-api-reference)
- [Contributing](#contributing)
  - [Prerequisites](#prerequisites)
  - [Build from source](#build-from-source)
  - [Run from source (no JAR needed)](#run-from-source-no-jar-needed)
  - [Module layout](#module-layout)
  - [How the database works](#how-the-database-works)
  - [Running tests](#running-tests)
  - [Code style](#code-style)

---

## Quick start — without Docker

> Best for local development and CI. No Docker daemon required.

**Prerequisites:** Java 17 or later (`java -version`).

**Step 1 — build the Next.js UI, then the fat JAR** (from the `apps/mock-server` root):

```bash
cd apps/mock-server/ui
npm ci && npm run build

cd ..
mvn package -pl standalone -am -DskipTests -q
```

The JAR lands at `standalone/target/mock-server-standalone-*.jar`. (Omit the UI step only if you pass `-Dskip.ui.copy=true` to bundle a backend-only JAR.)

**Step 2 — choose a data directory and run:**

```bash
# macOS / Linux
java -jar standalone/target/mock-server-standalone-*.jar \
  --spring.datasource.url="jdbc:h2:file:$HOME/.spec0/mock-server;DB_CLOSE_ON_EXIT=FALSE"
```

```powershell
# Windows PowerShell
java -jar standalone\target\mock-server-standalone-*.jar `
  --spring.datasource.url="jdbc:h2:file:$env:USERPROFILE\.spec0\mock-server;DB_CLOSE_ON_EXIT=FALSE"
```

The server starts on **http://localhost:8080**. Data is persisted to `~/.spec0/mock-server.mv.db`.
Restart the JAR with the same `--spring.datasource.url` and all your mock servers survive.

**Change the port:**

```bash
java -jar standalone/target/mock-server-standalone-*.jar \
  --server.port=9090 \
  --spring.datasource.url="jdbc:h2:file:$HOME/.spec0/mock-server;DB_CLOSE_ON_EXIT=FALSE"
```

---

## Quick start — with Docker

The Docker image stores its H2 database under `/data` inside the container. Mount a host
directory to that path to make data persistent across `docker stop` / `docker start`.

### macOS / Linux

```bash
mkdir -p ~/.spec0/mock-server-data

docker run -d \
  --name spec0-mock \
  -p 8080:8080 \
  -v ~/.spec0/mock-server-data:/data \
  spec0/mock-server:latest
```

### Windows (PowerShell)

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.spec0\mock-server-data"

docker run -d `
  --name spec0-mock `
  -p 8080:8080 `
  -v "$env:USERPROFILE\.spec0\mock-server-data:/data" `
  spec0/mock-server:latest
```

### Windows (Command Prompt)

```cmd
mkdir "%USERPROFILE%\.spec0\mock-server-data"

docker run -d ^
  --name spec0-mock ^
  -p 8080:8080 ^
  -v "%USERPROFILE%\.spec0\mock-server-data:/data" ^
  spec0/mock-server:latest
```

### WSL2

Run from inside the WSL2 terminal using the Linux syntax. Docker Desktop on Windows exposes the
Docker daemon to WSL2, so the same `docker run` command works:

```bash
mkdir -p ~/.spec0/mock-server-data

docker run -d \
  --name spec0-mock \
  -p 8080:8080 \
  -v ~/.spec0/mock-server-data:/data \
  spec0/mock-server:latest
```

> **Why `-v` matters:** without a volume mount the H2 database lives only inside the container
> layer. It survives `docker stop` / `docker start` on the *same container* but is lost on
> `docker rm` or image upgrade. Mounting a host directory gives you permanent persistence
> independent of the container lifecycle.

**Verify the server is healthy:**

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

**Open the bundled admin UI:**

```bash
open http://localhost:8080/ui/
```

**Stop / restart without losing data:**

```bash
docker stop spec0-mock
docker start spec0-mock
```

**Upgrade to a new image version:**

```bash
docker stop spec0-mock && docker rm spec0-mock
docker pull spec0/mock-server:latest
# Re-run the same `docker run` command with the same -v path — data is preserved
```

---

## Using with the UI

The open-source UI lives in **`apps/mock-server/ui`**. It is bundled into the JAR under
`/ui/` (same port as the API). For **contributor development**, run the API on 8080 and the
Next dev server on 3000 — the UI sends API calls to **8080** by default (see `ui/.env.example`).

### Run the UI in development

**Prerequisites:** Node 18+, mock server on port 8080.

```bash
# Terminal 1 — API (from apps/mock-server)
make dev-backend
# or: cd standalone && mvn spring-boot:run \
#   -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:/tmp/spec0-dev;DB_CLOSE_ON_EXIT=FALSE"

# Terminal 2 — Next dev (from apps/mock-server/ui)
cd ui && npm install && npm run dev
```

Open **http://localhost:3000** — API calls go to **http://localhost:8080** automatically.

### Production (fat JAR or Docker)

After `npm run build` in `ui/`, `mvn package` copies `ui/out` into the JAR. Open **http://localhost:8080/ui/** — API and UI share one origin.

### What the UI covers

**List page** (`/ui/`)

- All registered mock servers in a card grid
- Search by name
- **New Mock Server** button — upload or paste a YAML/JSON OpenAPI spec, set a name and
  response strategy, click Create. The spec is registered and variants auto-generated.

**Detail page** (`/ui/?mockServerId=<uuid>`)

Five tabs, same look and feel as the cloud platform:

| Tab | What you can do |
|---|---|
| **Overview** | See server status, mock base URL, and request activity |
| **Endpoints** | Per-operation enable/disable, strategy override, variant list, create/edit/delete variants, test panel to fire live requests |
| **Configuration** | Change default response strategy (RANDOM / SEQUENTIAL / ROUND_ROBIN / DEFAULT_ONLY), max variants, log retention |
| **Logs** | Paginated request log — operation ID, HTTP method, path, status code, response time |
| **Settings** | Rename the server, toggle enable/disable, delete (danger zone) |

**Enable / Disable toggle** in the header bar immediately enables or disables all mock traffic
for that server without navigating to Settings.

### PATCH endpoints added for UI operations

The following endpoints were added to the mock server REST layer (`io.spec0.mockserver` in `standalone/`) to support UI operations that the
original REST API did not expose:

```
PATCH /mock-server/servers/{id}
  { "name": "...", "isEnabled": true|false, "defaultStrategy": "RANDOM|..." }

GET   /mock-server/servers/{id}/operations
PATCH /mock-server/servers/{id}/operations/{operationId}
  { "isEnabled": true|false, "responseStrategy": "RANDOM|..." }
```

### Platform mode (spec0 cloud / self-hosted platform)

The same frontend also serves the cloud platform UI. In that mode it connects to
`platform-backend-core` (not the standalone server). To run the full platform UI:

1. Start the platform backend (defaults to port 8080).
2. Copy the example environment file and set the backend URL:
   ```bash
   cd apps/frontend
   cp .env.example .env.local
   # Set: NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
   ```
3. `npm run dev` → **http://localhost:3000**

Cloud mock server pages: `/apis/{apiId}/mock-server` and `/mock-servers/{mockServerId}`.

---

## REST API reference

All management endpoints are under `/mock-server/`. Mock traffic goes to `/mock/{mockServerId}/`.

### 1 — Register a spec

```bash
curl -X POST http://localhost:8080/mock-server/specs \
  -H "Content-Type: application/json" \
  -d '{
    "specName": "petstore",
    "specContent": "openapi: 3.0.0\ninfo:\n  title: Petstore\n  version: 1.0.0\npaths:\n  /pets:\n    get:\n      operationId: listPets\n      responses:\n        \"200\":\n          description: ok"
  }'
# Returns: { "specId": "<uuid>", ... }
```

`specContent` accepts both **YAML** and **JSON** OpenAPI specs. Re-posting the same name + content
is idempotent — you get back the same `specId`.

### 2 — Create a mock server

```bash
curl -X POST http://localhost:8080/mock-server/servers \
  -H "Content-Type: application/json" \
  -d '{
    "specId": "<specId from step 1>",
    "name": "petstore-mock",
    "defaultStrategy": "RANDOM"
  }'
# Returns: { "mockServerId": "<uuid>", ... }
```

`defaultStrategy` options: `RANDOM` (default), `SEQUENTIAL`, `ROUND_ROBIN`, `DEFAULT_ONLY`.

### 3 — Send mock requests

```bash
curl http://localhost:8080/mock/<mockServerId>/pets
```

Response headers tell you what happened:

| Header | Meaning |
|---|---|
| `X-spec0-Mock-Response: true` | Request was served by the mock engine |
| `X-spec0-Mock-Operation-Id` | The OpenAPI `operationId` that matched |
| `X-spec0-Mock-Variant-Id` | The UUID of the variant that was returned |

**Force a specific status code:**

```bash
curl -H "X-spec0-Preferred-Response-Code: 404" \
  http://localhost:8080/mock/<mockServerId>/pets
```

**Override the operation directly:**

```bash
curl -H "X-Mock-Operation-Id: listPets" \
  http://localhost:8080/mock/<mockServerId>/pets
```

### 4 — Manage variants

```bash
# List variants for a server
curl http://localhost:8080/mock-server/servers/<mockServerId>/variants

# List variants for a specific operation
curl "http://localhost:8080/mock-server/servers/<mockServerId>/variants?operationId=listPets"

# Create a custom variant
curl -X POST http://localhost:8080/mock-server/servers/<mockServerId>/variants \
  -H "Content-Type: application/json" \
  -d '{
    "operationId": "listPets",
    "responseName": "Empty list",
    "statusCode": "200",
    "responseBody": "[]",
    "isDefault": true,
    "displayOrder": 0
  }'

# Update a variant
curl -X PUT http://localhost:8080/mock-server/servers/<mockServerId>/variants/<variantId> \
  -H "Content-Type: application/json" \
  -d '{ "operationId": "listPets", "responseName": "Error", "statusCode": "500", "responseBody": "{\"error\":\"oops\"}", "isDefault": false, "displayOrder": 1 }'

# Delete a variant
curl -X DELETE http://localhost:8080/mock-server/servers/<mockServerId>/variants/<variantId>
```

### 5 — View request logs

```bash
curl "http://localhost:8080/mock-server/servers/<mockServerId>/logs?limit=20"
```

### 6 — Export a mock server

Export captures the spec, server config, and all variants in a single portable JSON file.

```bash
curl http://localhost:8080/mock-server/servers/<mockServerId>/export > my-mock.json
```

### 7 — Update a mock server

```bash
# Rename
curl -X PATCH http://localhost:8080/mock-server/servers/<mockServerId> \
  -H "Content-Type: application/json" \
  -d '{ "name": "new-name" }'

# Enable / disable
curl -X PATCH http://localhost:8080/mock-server/servers/<mockServerId> \
  -H "Content-Type: application/json" \
  -d '{ "isEnabled": false }'

# Change default response strategy
curl -X PATCH http://localhost:8080/mock-server/servers/<mockServerId> \
  -H "Content-Type: application/json" \
  -d '{ "defaultStrategy": "DEFAULT_ONLY" }'
```

### 8 — Manage operation configs

```bash
# List all operation configs for a server
curl http://localhost:8080/mock-server/servers/<mockServerId>/operations

# Disable a specific operation (it will return 404 for that path)
curl -X PATCH http://localhost:8080/mock-server/servers/<mockServerId>/operations/listPets \
  -H "Content-Type: application/json" \
  -d '{ "isEnabled": false }'

# Override response strategy for one operation
curl -X PATCH http://localhost:8080/mock-server/servers/<mockServerId>/operations/listPets \
  -H "Content-Type: application/json" \
  -d '{ "responseStrategy": "SEQUENTIAL" }'
```

### 9 — Other server operations

```bash
# List all mock servers
curl http://localhost:8080/mock-server/servers

# Get a single mock server
curl http://localhost:8080/mock-server/servers/<mockServerId>

# Delete a mock server
curl -X DELETE http://localhost:8080/mock-server/servers/<mockServerId>
```

---

## Contributing

### Prerequisites

| Tool | Minimum version |
|---|---|
| Java JDK | 17 |
| Maven | 3.9 |

No database install required — H2 is embedded.

### Build from source

From the `apps/mock-server` directory, build the UI first, then Maven:

```bash
cd ui && npm ci && npm run build && cd ..
mvn package -pl standalone -am -DskipTests
```

The resulting JAR is at `standalone/target/mock-server-standalone-*.jar`.

**Docker image** (UI + JAR in one image): build the `*-exec.jar` first, then `docker build` (see [`../Dockerfile`](../Dockerfile) and [Docker](../README.md#docker-single-container-api--ui) in the repo README).

### Run from source (no JAR needed)

Use the Spring Boot Maven plugin for a fast dev loop — no `mvn package` required on every change:

```bash
cd standalone

mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:/tmp/spec0-dev;DB_CLOSE_ON_EXIT=FALSE"
```

The server starts on **http://localhost:8080**. Edit sources in `standalone/` (or `engine/` for pure Java) or
`mock-server-standalone`, re-run, and the changes take effect immediately.

To use a different port:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=9090 --spring.datasource.url=jdbc:h2:file:/tmp/spec0-dev;DB_CLOSE_ON_EXIT=FALSE"
```

### Module layout

```
apps/mock-server/
├── pom.xml
├── engine/                   Pure Java: mockgen (`io.spec0.mockserver.mockgen`), engine services
├── core/                     Spring Boot starter — engine + REST API
├── standalone/               Self-hosted app — core + H2 + bundled static UI
├── ui/                       Next.js admin UI (static export → JAR classpath:/static/ui/)
└── Dockerfile                Multi-stage build (Node + Maven + JRE)
```

**Key design rule:** shared mock server packages (`io.spec0.mockserver`) have **zero** references to platform concepts (orgs, teams,
API IDs). The platform couples to mock servers via `api_mock_server_associations` — a plain UUID
reference with no foreign key constraint.

### How the database works

The standalone app uses an H2 file-based database. Schema is managed by Flyway, not Hibernate.

**Flyway setup:**
- Migrations live in `standalone/src/main/resources/db/mock-server/`
- History table: `flyway_mock_server_history` (isolated from any platform Flyway history)
- Schema: `mock_server`
- The Flyway bean (`mockServerFlyway`) is declared in `MockServerAutoConfiguration` — it runs in
  both standalone (H2) and platform-embedded (Postgres) modes without configuration changes

**`spring.flyway.enabled: false`** is set in the standalone's `application.yaml` intentionally.
The named `mockServerFlyway` bean (declared in the auto-configuration) owns all migrations.
Do not re-enable the default Flyway — it would conflict on version numbers.

**Schema location override** — the default database path is `/data/spec0-mock` (Docker volume).
Override it with `--spring.datasource.url` on the command line for local runs:

```bash
--spring.datasource.url="jdbc:h2:file:/your/path/spec0-mock;DB_CLOSE_ON_EXIT=FALSE"
```

Do not add `AUTO_SERVER=TRUE` — H2 2.x does not allow it together with `DB_CLOSE_ON_EXIT=FALSE`.

### Running tests

```bash
mvn test -pl core
mvn test -pl standalone
```

### Code style

The project uses [Google Java Format](https://github.com/google/google-java-format) enforced by
the Spotless Maven plugin.

Check formatting:

```bash
mvn spotless:check -pl core
```

Apply formatting:

```bash
mvn spotless:apply -pl core
```

Spotless runs automatically on `mvn verify`. A CI build will fail if formatting is not applied.
