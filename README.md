# spec0 Mock Server

Self-hosted OpenAPI mock server. Register a spec, get instant mock responses, manage variants and logs through a bundled web UI — no account required.

[![CI](https://github.com/spec-0/mock-server/actions/workflows/ci.yml/badge.svg)](https://github.com/spec-0/mock-server/actions/workflows/ci.yml)
[![GHCR](https://img.shields.io/badge/ghcr.io-spec--0%2Fmock--server-blue?logo=github)](https://github.com/spec-0/mock-server/pkgs/container/mock-server)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

## Quick start

```bash
docker run -d -p 8080:8080 -v spec0-mock-data:/data ghcr.io/spec-0/mock-server
# open http://localhost:8080/ui/
```

Or with Docker Compose (data persists across restarts):

```bash
docker compose up -d
# http://localhost:8080/ui/
```

---

## Features

- **Auto-generated responses** from your OpenAPI spec — no setup beyond uploading the spec
- **[Variant management](docs/variants-and-strategies.md)** — define multiple named responses per operation with `RANDOM`, `SEQUENTIAL`, `ROUND_ROBIN`, or `DEFAULT_ONLY` strategies
- **[Schema validation](docs/schema-validation.md)** (`OFF` / `WARN` / `STRICT`) — validate variant bodies against the OpenAPI response schema before saving
- **[CEL expressions](docs/cel-expressions.md)** — dynamic responses evaluated at request time using request path params, query params, headers, and body
- **[Request logging](docs/request-logs.md)** — inspect full request/response pairs per mock server with infinite-scroll log viewer
- **[MCP integration](docs/mcp-integration.md)** — manage mock servers from Claude, Cursor, or any MCP-compatible AI assistant
- **[Drop-in base URL](docs/using-the-mock-url.md)** — point your app at `/mock/{serverId}` and all requests are intercepted; copy curl commands directly from the UI
- **Web UI** — bundled at `/ui/`, no separate frontend deployment

→ **[Full documentation](docs/README.md)**

---

## Schema validation modes

Set per mock server via `PATCH /mock-server/servers/{id}/config`:

| Mode | Behavior |
|------|----------|
| `OFF` (default) | No validation. Any response body is accepted. |
| `WARN` | Body is saved; violations are logged and returned in `validationWarnings`. |
| `STRICT` | Body is rejected with `400` if it violates the response schema. |

Example — enable STRICT:

```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{id}/config \
  -H 'Content-Type: application/json' \
  -d '{"schemaValidationMode":"STRICT"}'
```

---

## Configuration

Override any Spring Boot property via environment variables (relaxed binding):

| Env var | Default | Purpose |
|---------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:/data/spec0-mock;DB_CLOSE_ON_EXIT=FALSE` | Database location |
| `SERVER_PORT` | `8080` | HTTP port |
| `JAVA_OPTS` | _(empty)_ | Extra JVM flags (e.g. `-Xmx512m`) |

Example — custom port:

```bash
docker run -d -p 9090:9090 -e SERVER_PORT=9090 -v spec0-mock-data:/data spec0/mock-server
# http://localhost:9090/ui/
```

---

## Local development

Prerequisites: Java 17+, Maven 3.9+, Node 18+

Run two terminals from the repo root:

```bash
# Terminal 1 — backend on http://localhost:8080
make dev-backend

# Terminal 2 — UI on http://localhost:3000
make dev-ui          # first time: cd ui && npm ci
```

Build the fat JAR (API + UI bundled):

```bash
cd ui && npm ci && npm run build && cd ..
mvn -pl standalone -am package -DskipTests
java -jar standalone/target/mock-server-standalone-*-exec.jar
```

Run all tests:

```bash
./mvnw verify -Dskip.ui.copy=true
```

### Docker REST E2E

End-to-end tests hit a **real** HTTP server (not `@SpringBootTest` in-process). The script builds the fat JAR, builds the image, runs a container, waits for `/actuator/health`, runs `mock-server-e2e-tests` against that base URL, then removes the container.

```bash
chmod +x scripts/e2e-against-docker.sh   # once
./scripts/e2e-against-docker.sh
```

Optional: `MOCK_SERVER_E2E_PORT=19090 ./scripts/e2e-against-docker.sh`, or `SKIP_UI_BUILD=1` if `ui/out` is already present.

To run the E2E test module manually against any running instance:

```bash
./mvnw -f e2e-tests/pom.xml test -Dmock.server.baseUrl=http://localhost:8080
```

---

## API reference

The full REST contract is available at `http://localhost:8080/swagger-ui.html` on a running instance, or statically at [`openapi.yaml`](openapi.yaml).

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and feature requests welcome via [GitHub Issues](https://github.com/spec0/mock-server/issues).

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
