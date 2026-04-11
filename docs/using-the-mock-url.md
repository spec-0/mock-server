# Using the Mock URL in Your Application

## How it works

Every mock server gets a base URL of the form:

```
http://<host>:<port>/mock/<mockServerId>
```

Point your application's HTTP client at this URL instead of the real API. All requests are routed to the mock server, matched against your OpenAPI spec, and answered with the active variant for that operation.

For a mock server running locally with ID `550e8400-e29b-41d4-a716-446655440000`, the base URL is:

```
http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000
```

A request to `GET /users/123` in your app becomes:

```
GET http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000/users/123
```

The mock server finds the matching operation from your OpenAPI spec (using path parameter extraction), selects a variant based on the current strategy, and returns the response.

---

## Configuring your application

Set the API base URL in your application's configuration to point at the mock server. Most HTTP clients and SDK configurations accept a base URL parameter:

```bash
# Environment variable approach
API_BASE_URL=http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000
```

```javascript
// JavaScript / TypeScript
const client = new ApiClient({
  baseUrl: 'http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000',
});
```

```python
# Python
client = ApiClient(base_url="http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000")
```

No other changes are needed. The mock server accepts all HTTP methods and preserves path parameters, query parameters, headers, and request bodies exactly as sent.

---

## Copying the curl command from the UI

The **Endpoints** tab provides a built-in request builder for each operation. After filling in parameters and a request body, you can:

- Click **Send** to fire the request directly from the browser.
- Click the **copy curl** button (terminal icon, next to Send) to copy the equivalent `curl` command to your clipboard.

The generated command includes:
- The full mock URL for that operation
- `Content-Type: application/json`
- The `X-Mock-Operation-Id` header (used for unambiguous operation matching)
- Any custom headers you added in the builder
- The request body (for POST, PUT, PATCH, DELETE)

Example output:

```bash
curl -sS -X POST \
  'http://localhost:8080/mock/550e8400-e29b-41d4-a716-446655440000/users' \
  -H 'Content-Type: application/json' \
  -H 'X-Mock-Operation-Id: createUser' \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

Paste this directly into a terminal or into your integration test setup.

---

## Request headers understood by the mock server

| Header | Effect |
|--------|--------|
| `X-Mock-Operation-Id` | Skip path matching and use this `operationId` directly. Useful when path is ambiguous. |
| `X-spec0-Preferred-Response-Code` | Return the first variant with this status code, ignoring the current strategy. E.g., `404` to force an error response. |

---

## Supported methods

The mock server handles all standard HTTP methods on every operation path:

`GET` · `POST` · `PUT` · `PATCH` · `DELETE` · `OPTIONS`

---

## Docker and network access

When the mock server runs in Docker and your application runs in a separate container (or on the host), use the appropriate network address:

| Setup | Base URL |
|-------|----------|
| Both on host | `http://localhost:8080/mock/{mockServerId}` |
| App in Docker, mock on host | `http://host.docker.internal:8080/mock/{mockServerId}` |
| Both in same Docker Compose network | `http://mock-server:8080/mock/{mockServerId}` |
