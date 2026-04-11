# Mock server OpenAPI schema validation

Standalone mock-server can validate **JSON request bodies** at runtime and **static variant response bodies** on save, using the same OpenAPI document stored as `api_specs.spec_content`.

## Supported stacks

| Layer | Technology | Notes |
|-------|------------|--------|
| OpenAPI parse | [swagger-parser](https://github.com/swagger-api/swagger-parser) (`OpenAPIV3Parser`, `resolve`) | Used for registration, operation list, and validation module cache |
| JSON Schema | [networknt/json-schema-validator](https://github.com/networknt/json-schema-validator) **2.0.x** | Aligns with MCP Java SDK / Spring AI; uses `SchemaRegistry` + OpenAPI dialects |
| Dialects | `Dialects.getOpenApi30()` / `getOpenApi31()` | Selected from the document `openapi` field (`3.0.x` vs `3.1.x`) |

## Pinned dependency versions (see `mock-server/pom.xml`)

| Artifact | Property / version |
|----------|---------------------|
| `io.swagger.parser.v3:swagger-parser` | `swagger-parser.version` (e.g. **2.1.24**) |
| `com.networknt:json-schema-validator` | `json-schema-validator.version` (**2.0.x**, aligned with Spring AI MCP / [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)) |

## OpenAPI versions

| OpenAPI | Status |
|---------|--------|
| **3.0.x** | Supported for typical API contracts; dialect OpenAPI 3.0 |
| **3.1.x** | Supported; dialect OpenAPI 3.1 |

Unit tests load minimal fixtures: `openapi-validation/src/test/resources/openapi-30-minimal.yaml` and `openapi-31-minimal.yaml` (one per lineage).

Very large specs may require tuning `mockserver.openapi-cache.*` in `application.yaml` (TTL, max size).

## Behaviour

- **OFF**: No validation.
- **WARN**: Log validation failures; do not block requests or variant saves.
- **STRICT**: Reject invalid request JSON (HTTP 4xx) or variant save with `IllegalArgumentException`.
- **CEL variants**: Response body is not validated at save time; runtime response validation is out of scope for v1.
- **Cache**: Parsed specs are cached per `specId`; new registrations call `ParsedOpenApiCache.invalidate(specId)` after insert.

## API

- `GET /mock-server/servers/{mockServerId}/config` — includes `schemaValidationMode` (`OFF` | `WARN` | `STRICT`).
- `PATCH /mock-server/servers/{mockServerId}/config` — update `schemaValidationMode`, limits, `defaultStrategy`, etc.
