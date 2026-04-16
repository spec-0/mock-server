# spec0 Mock Server — Documentation

A self-hosted OpenAPI mock server. Register a spec, get instant mock responses, and control every detail through the web UI or the REST API — no account required.

---

## Contents

### Getting started

| | |
|---|---|
| [Quick start](../README.md#quick-start) | Run with Docker in 30 seconds |
| [UI walkthrough (video)](../README.md#ui-walkthrough) | Screen recording of the bundled `/ui/` after startup |
| [Using the mock URL](./using-the-mock-url.md) | Point your app at the mock server, copy curl commands |

### Core concepts

| | |
|---|---|
| [Variants & Response Strategies](./variants-and-strategies.md) | Define multiple responses per operation; choose `RANDOM`, `SEQUENTIAL`, `ROUND_ROBIN`, or `DEFAULT_ONLY` |
| [CEL Expressions](./cel-expressions.md) | Write dynamic response logic evaluated at request time |
| [Schema Validation](./schema-validation.md) | Validate variant bodies against your OpenAPI spec (`OFF` / `WARN` / `STRICT`) |

### Observability

| | |
|---|---|
| [Request Logs](./request-logs.md) | Inspect full request/response pairs, headers, timings, and selected variants |

### Integrations

| | |
|---|---|
| [MCP — Claude, Cursor & AI Assistants](./mcp-integration.md) | Manage mock servers from any MCP-compatible AI assistant |

---

## How the pieces fit together

```
Your OpenAPI spec
       │
       ▼
  Mock Server
  ┌──────────────────────────────────────┐
  │  Operations  (from spec)             │
  │      └── Variants  (you define)      │
  │              ├── Static body         │
  │              └── CEL expression      │
  │                                      │
  │  Response Strategy  (per server      │
  │  or per operation)                   │
  │      RANDOM · SEQUENTIAL ·           │
  │      ROUND_ROBIN · DEFAULT_ONLY      │
  │                                      │
  │  Schema Validation  (optional)       │
  │      OFF · WARN · STRICT             │
  │                                      │
  │  Request Logs  (automatic)           │
  └──────────────────────────────────────┘
       │
       ▼
  /mock/{serverId}/{path}
  ← your application hits this URL
```

Every request to `/mock/{serverId}` is matched against the spec, a variant is selected by the active strategy, and the response is logged.
