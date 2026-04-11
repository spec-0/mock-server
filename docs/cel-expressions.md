# CEL Expressions

**CEL (Common Expression Language)** lets you write dynamic response logic that is evaluated at request time rather than saved as a static body. With CEL you can:

- Return different responses based on path parameters, query parameters, or headers
- Generate random UUIDs or timestamps in every response
- Simulate conditional logic (e.g., return `404` for unknown IDs)

CEL is sandboxed and Turing-incomplete, making it safe to execute on the server.

## Contents

- [Creating a CEL variant](#creating-a-cel-variant)
- [Expression context](#expression-context)
- [Built-in functions](#built-in-functions)
- [Return value](#return-value)
- [Examples](#examples)
- [Common mistakes](#common-mistakes)

---

## Creating a CEL variant

### From the UI

1. Go to **Endpoints** → expand an operation → **Add Variant**.
2. Switch to the **CEL** tab.
3. Write your expression in the editor.
4. Save. CEL variants are marked with a sparkle badge in the variant list.

### From the API

Set `celExpression` instead of (or in addition to) `responseBody`:

```bash
curl -X POST http://localhost:8080/mock-server/servers/{mockServerId}/variants \
  -H 'Content-Type: application/json' \
  -d '{
    "operationId": "getUser",
    "responseName": "Dynamic",
    "statusCode": "200",
    "celExpression": "request.path_params.id == '\''99'\'' ? {'\''status'\'': 404, '\''body'\'': {'\''error'\'': '\''not found'\''}} : {'\''status'\'': 200, '\''body'\'': {'\''id'\'': request.path_params.id}}"
  }'
```

---

## Expression context

Every CEL expression receives a `request` object and an `env` map.

### `request` fields

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `request.method` | `string` | HTTP method | `"GET"`, `"POST"` |
| `request.path` | `string` | Full request path | `"/users/123"` |
| `request.path_params` | `map<string, string>` | Named path parameters | `request.path_params.id` |
| `request.query_params` | `map<string, string>` | Query string parameters | `request.query_params.filter` |
| `request.headers` | `map<string, string>` | Request headers (keys lowercased) | `request.headers["x-session-id"]` |
| `request.body` | `map` or `null` | Parsed JSON request body | `request.body.name` |

### `env` variables

Per-server environment variables are accessible as `env.<KEY>`. Define them in **Settings → Environment Variables**.

```
env.REGION          // e.g. "us-east-1"
env.API_VERSION     // e.g. "v2"
```

---

## Built-in functions

| Function | Returns | Description |
|----------|---------|-------------|
| `uuid()` | `string` | A random UUID v4 |
| `now()` | `string` | Current timestamp as ISO 8601 |
| `randomInt(min, max)` | `int` | Random integer in `[min, max)` |

---

## Return value

> [!IMPORTANT]
> Your expression must return a **map with single-quoted string keys**. Unquoted identifiers are treated as variable references and will fail if the variable is not defined.

```
{
  'status':  <int>,                  // required — HTTP status code
  'body':    <any>,                  // optional — response body
  'headers': <map<string, string>>   // optional — extra response headers
}
```

---

## Examples

### Branch on a path parameter

```cel
request.path_params.id == "99"
  ? {'status': 404, 'body': {'error': 'user not found'}}
  : {'status': 200, 'body': {'id': request.path_params.id, 'name': 'Alice'}}
```

### Generated ID and timestamp on every response

```cel
{
  'status': 201,
  'body': {
    'id': uuid(),
    'createdAt': now(),
    'name': request.body.name
  }
}
```

### Branch on a query parameter

```cel
request.query_params.status == "inactive"
  ? {'status': 200, 'body': {'active': false, 'reason': 'account suspended'}}
  : {'status': 200, 'body': {'active': true}}
```

### Echo back a request header

```cel
{
  'status': 200,
  'body': {'sessionId': request.headers["x-session-id"], 'ok': true}
}
```

### Add custom response headers

```cel
{
  'status': 200,
  'body': {'result': 'ok'},
  'headers': {'X-Request-Id': uuid(), 'X-Served-By': 'spec0-mock'}
}
```

### Use a server-level environment variable

```cel
{
  'status': 200,
  'body': {'region': env.REGION, 'id': request.path_params.id}
}
```

---

## Common mistakes

> [!WARNING]
> **`request.body` is `null` for GET requests and other bodyless methods.** Guard against it before accessing fields:
> ```cel
> request.body != null ? request.body.userId : 'anonymous'
> ```

> [!WARNING]
> **Path parameters are always strings.** Compare with string literals, not integers:
> ```cel
> request.path_params.id == "42"   // correct
> request.path_params.id == 42     // wrong — type mismatch, will always be false
> ```

> [!NOTE]
> **CEL variants bypass response-body schema validation at save time.** Validation only applies to static variants. The body returned by a CEL expression is not checked against the OpenAPI schema.

---

## See also

- [Variants & Response Strategies](./variants-and-strategies.md) — where CEL variants fit into the overall variant model
- [← Documentation index](./README.md)
