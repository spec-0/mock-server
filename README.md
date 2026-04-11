# spec0 Mock Server (open source)

Self-hosted OpenAPI mock server: register a spec, get auto-generated responses, manage variants and logs through a bundled web UI. No platform account required.

| What | Where |
|------|--------|
| REST contract | [`openapi.yaml`](openapi.yaml) |
| TypeScript types | `ui` — run `npm run generate-types` (see [Building artifacts locally](#building-artifacts-locally)) |
| **Production / Docker** | API + static UI on **one port** — UI at **`/ui/`**, API under `/mock-server/**` and `/mock/{id}/**` |

---

## Prerequisites

| Tool | Notes |
|------|--------|
| **Java 17+** | Backend (`java -version`) |
| **Maven 3.9+** | Build and `spring-boot:run` |
| **Node 18+** | UI only (`node -version`) |
| **Docker** | Optional — for container workflow |

---

## Local development (backend + UI separately)

Use this when you change Java or TypeScript and want fast feedback. Run **two terminals** from `apps/mock-server`.

### 1. Backend (Spring Boot)

Default API URL: **http://localhost:8080**

**Option A — Makefile (H2 file DB under `standalone/target/`):**

```bash
make dev-backend
```

**Option B — Maven (custom data path or port):**

```bash
cd standalone
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:/tmp/spec0-mock-dev;DB_CLOSE_ON_EXIT=FALSE --server.port=8080"
```

- Change persistence: set `--spring.datasource.url` to any H2 file URL you prefer.
- Change port: add `--server.port=9090` and point the UI at that port (see [UI environment variables](#ui-environment-variables)).

### 2. Frontend (Next.js dev server)

Default: **http://localhost:3000** — the app talks to the API at **http://localhost:8080** without extra config (ports `3000` / `3001` are detected automatically).

```bash
make dev-ui
# same as: cd ui && npm run dev
```

**First time only:**

```bash
cd ui && npm ci
```

### 3. UI environment variables (optional)

Create **`ui/.env.local`** (gitignored) — copy from [`ui/.env.example`](ui/.env.example).

| Variable | When to set |
|----------|-------------|
| `NEXT_PUBLIC_MOCK_SERVER_API_URL` | API not on `http://localhost:8080` (e.g. different host or port). Example: `http://localhost:9090` |
| `NEXT_PUBLIC_API_BASE_URL` | Alias for the same — use **one** of these, not both |
| `NEXT_PUBLIC_STANDALONE_URL` | Legacy alias; prefer `NEXT_PUBLIC_MOCK_SERVER_API_URL` |

Resolution order in the app:

1. `NEXT_PUBLIC_MOCK_SERVER_API_URL` / `NEXT_PUBLIC_API_BASE_URL` / `NEXT_PUBLIC_STANDALONE_URL` (baked in at **Next build time** for production static export)
2. Dev only: if the page is served on port **3000** or **3001**, default API is **`http://<same-host>:8080`**
3. Otherwise: same origin as the page (used when you open the UI from the JAR/Docker on port 8080)

After editing `.env.local`, restart `npm run dev`.

### Helper commands (Makefile)

From **`apps/mock-server`**:

| Target | Purpose |
|--------|---------|
| `make help` | List targets |
| `make dev-backend` | Run API on 8080 with dev H2 DB |
| `make dev-ui` | Run Next on 3000 |
| `make generate-types` | Regenerate `ui/lib/api/generated.ts` from `openapi.yaml` |

Shell scripts (same repo folder):

| Script | Purpose |
|--------|---------|
| [`scripts/dev-backend.sh`](scripts/dev-backend.sh) | Wrapper: run Spring Boot with dev defaults |
| [`scripts/dev-ui.sh`](scripts/dev-ui.sh) | Wrapper: `npm run dev` in `ui/` |

---

## Docker (single container: API + UI)

The image **only packages a pre-built executable JAR** (no Maven or Node inside `docker build`). Build the UI and JAR in CI or locally first — same artifact as `java -jar standalone/target/...-exec.jar`.

### Build the image

1. Produce **`standalone/target/mock-server-standalone-*-exec.jar`** (includes static UI under `/ui/` when `ui/out` was present at package time):

   ```bash
   cd ui && npm ci && npm run build && cd ..
   mvn -pl standalone -am package -DskipTests
   ```

2. From the **`mock-server/`** directory (build context must include that JAR path):

   ```bash
   docker build -t spec0/mock-server:latest .
   ```

**GitHub Actions:** run the UI + Maven steps in a job, then `docker build` with the `mock-server/` directory as context. [`.dockerignore`](.dockerignore) keeps the context small (only the `*-exec.jar` is included from `standalone/target/`, not other build outputs).

### Run with Compose (recommended)

```bash
docker compose up -d
```

- **UI:** http://localhost:8080/ui/
- **Health:** http://localhost:8080/actuator/health  
- Data persists in the named volume **`mock-server-data`** (H2 files under `/data` in the container).

Stop / remove:

```bash
docker compose down        # keeps volume
docker compose down -v     # removes volume (deletes DB)
```

### Run with `docker run`

```bash
docker run -d --name spec0-mock -p 8080:8080 -v spec0-mock-data:/data spec0/mock-server:latest
```

### Container environment (Spring Boot)

Override any Spring Boot 3.x property with env vars (relaxed binding), e.g.:

| Env | Example | Purpose |
|-----|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:/data/spec0-mock;DB_CLOSE_ON_EXIT=FALSE` | DB location (default matches `/data` volume) |
| `SERVER_PORT` | `8080` | HTTP port inside the container (map with `-p host:container`) |

Example custom port:

```bash
docker run -d -p 9090:9090 -e SERVER_PORT=9090 -v spec0-mock-data:/data spec0/mock-server:latest
```

Then open **http://localhost:9090/ui/**.

---

## Building artifacts locally

### Regenerate TypeScript types (`openapi.yaml` → `ui/lib/api/generated.ts`)

```bash
make generate-types
# or: cd ui && npm run generate-types
```

Commit `ui/lib/api/generated.ts` when the API contract changes.

### Production UI bundle (`ui/out`)

Required before packaging the JAR **with** the UI:

```bash
cd ui && npm ci && npm run build
```

### Fat JAR (API + bundled static UI)

From **`mock-server/`**, after `ui/out` exists:

```bash
mvn -pl standalone -am package -DskipTests
```

Output: executable **`standalone/target/mock-server-standalone-*-exec.jar`** (the plain `mock-server-standalone-*.jar` without classifier is the library JAR for embedders).

```bash
java -jar standalone/target/mock-server-standalone-*-exec.jar
# http://localhost:8080/ui/
```

### JAR without UI (backend-only)

```bash
mvn -pl standalone -am package -DskipTests -Dskip.ui.copy=true
```

---

## Embedding in another Spring Boot app

The old **`mock-server-core`** starter and separate **`openapi-mockgen`** artifact are removed. Use **`io.spec0:mock-server-engine`** (pure Java) plus **`io.spec0:mock-server-standalone`** for the shared Spring layer (`io.spec0.mockserver.*`). Import **`io.spec0.mockserver.config.MockServerConfiguration`** for Flyway + JPA scoped to the mock schema, and component-scan **`io.spec0.mockserver.controller`** and **`io.spec0.mockserver.service`** (omit **`io.spec0.mockserver.standalone`** unless you want the self-hosted app and UI wiring). Exclude H2 and Spring AI MCP from the standalone dependency if your app uses Postgres and does not need MCP.

---

## Open source vs cloud

The OSS distribution includes the full mock engine, persistence, and admin UI for local or self-hosted use. **spec0 Cloud** adds team and org features, hosted operations, and platform integrations — upgrade when you need those, without giving up local mocks.

---

## More detail

- Database, Flyway, and REST examples: [`standalone/README.md`](standalone/README.md)

## License

See the repository root for the license applicable to this project.
