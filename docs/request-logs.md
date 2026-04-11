# Request Logs

The mock server records every incoming request and the response it returned. Logs are available in the **Logs** tab of each mock server and via the REST API.

---

## What is captured

Each log entry contains:

| Field | Description |
|-------|-------------|
| `operationId` | The OpenAPI operation that was matched |
| `httpMethod` | HTTP method of the request |
| `requestPath` | Path from the incoming request |
| `requestHeaders` | All request headers |
| `requestBody` | The request body (if present) |
| `responseStatusCode` | Status code that was returned |
| `responseHeaders` | All response headers (including `X-spec0-*` diagnostic headers) |
| `responseBody` | Response body that was returned |
| `variantId` | UUID of the variant that was selected |
| `responseTimeMs` | Time taken to produce the response, in milliseconds |
| `clientIp` | IP address of the caller |
| `userAgent` | User-Agent header from the caller |
| `createdAt` | Timestamp of the request (ISO 8601) |

---

## Viewing logs in the UI

1. Open a mock server and go to the **Logs** tab.
2. The most recent requests appear at the top, newest first.
3. Scroll down to load older entries (infinite scroll, 20 entries per page).
4. Click any row to expand it and inspect the full request and response, including headers and body.

---

## Querying logs via API

```bash
# Get the 50 most recent log entries
curl http://localhost:8080/mock-server/servers/{mockServerId}/logs?limit=50
```

The response is an array of log objects sorted by `createdAt` descending.

---

## Diagnostic response headers

Every mock response includes these headers, which also appear in the log:

| Header | Value |
|--------|-------|
| `X-spec0-Mock-Response` | `true` |
| `X-spec0-Mock-Variant-Id` | UUID of the selected variant |
| `X-spec0-Mock-Operation-Id` | Resolved `operationId` |

> [!TIP]
> Use `X-spec0-Mock-Variant-Id` in your test assertions to verify that the expected variant was served for a given request.

---

## Log retention

The number of log entries retained per mock server is controlled by the `maxLogEntries` configuration field. Once the limit is reached, older entries are dropped.

```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{mockServerId}/config \
  -H 'Content-Type: application/json' \
  -d '{"maxLogEntries": 500}'
```

---

## See also

- [Variants & Response Strategies](./variants-and-strategies.md) — understand the `variantId` field in each log entry
- [Using the mock URL](./using-the-mock-url.md) — how requests reach the mock server
- [← Documentation index](./README.md)
