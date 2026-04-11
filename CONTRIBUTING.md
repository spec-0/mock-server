# Contributing to spec0 Mock Server

Thank you for your interest in contributing!

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ (or use `./mvnw`) |
| Node | 18+ |
| Docker | Optional |

## Local development

Run two terminals from the repo root:

**Terminal 1 — backend (http://localhost:8080):**
```bash
make dev-backend
```

**Terminal 2 — UI (http://localhost:3000):**
```bash
make dev-ui
```

First time only: `cd ui && npm ci`

## Running tests

```bash
# All tests
./mvnw test

# Standalone module only (includes integration tests)
./mvnw -pl standalone test -Dskip.ui.copy=true

# Engine/validation only
./mvnw -pl engine,openapi-validation test

# Single test class
./mvnw -pl standalone test -Dtest="SchemaValidationIntegrationTest" -Dskip.ui.copy=true
```

## Code style

Java is enforced by [Spotless](https://github.com/diffplug/spotless) with Google Java Format:

```bash
./mvnw spotless:apply   # auto-format
./mvnw spotless:check   # verify (runs in CI)
```

The CI workflow runs `spotless:check` on every PR. Format before pushing.

## Submitting a pull request

1. Fork the repo and create a branch from `main`.
2. Make your changes with tests.
3. Run `./mvnw verify -Dskip.ui.copy=true` and `./mvnw spotless:check` — both must pass.
4. Open a PR against `main`. Fill in the PR template.

## Project structure

| Module | Description |
|--------|-------------|
| `engine/` | Pure Java mock engine — no Spring, no I/O |
| `openapi-validation/` | Framework-agnostic OpenAPI schema validator |
| `standalone/` | Spring Boot app: JPA, Flyway, REST controllers |
| `ui/` | Next.js static export (served under `/ui/`) |

## Questions?

Open a [question issue](https://github.com/spec0/mock-server/issues/new?template=question.md).
