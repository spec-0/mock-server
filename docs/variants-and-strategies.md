# Variants and Response Strategies

Every operation in your OpenAPI spec can have one or more **variants** — named, pre-configured responses that the mock server returns when that operation is called. Variants let you simulate different scenarios (happy path, error states, edge cases) without changing any configuration in your application.

## Contents

- [What is a variant?](#what-is-a-variant)
- [Generated default responses](#generated-default-responses)
- [Creating variants](#creating-variants)
- [Response strategies](#response-strategies)
- [Forcing a specific status code](#forcing-a-specific-status-code)
- [Managing variants](#managing-variants)

---

## What is a variant?

A variant has the following fields:

| Field | Description |
|-------|-------------|
| **Name** | Human-readable label, e.g. `"Success"`, `"Not Found"`, `"Rate Limited"` |
| **Status code** | HTTP status code returned, e.g. `200`, `404`, `429` |
| **Response body** | JSON body returned in the response |
| **Headers** | Optional extra response headers |
| **Default flag** | Whether this is the default for the `DEFAULT_ONLY` strategy |
| **CEL expression** | Optional dynamic expression evaluated at request time (see [CEL Expressions](./cel-expressions.md)) |

---

## Generated default responses

When you create a mock server, it makes one **default variant** for every operation. The body of that variant is built from your spec, so you get a working response without writing anything.

The mock server uses the data you already wrote in the spec whenever it can. For each value it picks the first of these that exists:

1. An `example` (or the first of the named `examples`) on the response's media type, e.g. under `application/json`.
2. An `example` on the schema. This works on component schemas used through `$ref`, on array `items`, and on each property.
3. The first entry of the schema's `examples` list (OpenAPI 3.1).
4. The schema's `default`.
5. The schema's `const`.
6. The first value of the schema's `enum`.
7. A made-up value that fits the `format`, e.g. a real-looking email for `format: email`, or a UUID for `format: uuid`.
8. A random placeholder value (a short word, a number, `true`/`false`).

Objects and arrays are filled in one field at a time with the same rules, so nested objects and lists also use your examples. `allOf` parts are merged into one object; for `oneOf` and `anyOf` the first option is used.

For example, with this schema:

```yaml
Book:
  type: object
  properties:
    id: { type: string, example: "b-1" }
    title: { type: string, example: "The Pragmatic Programmer" }
```

a `GET /books` that returns an array of `Book` gives `[{"id": "b-1", "title": "The Pragmatic Programmer"}]`.

A few things to know:

- The body is built **once**, when the mock server is created. Every request then returns the same stored body. Change the spec and recreate the mock server (or edit the variant) to get a new one.
- Values that come from your spec are always the same. Only fields with no example, default, const or enum get random values, and those are chosen once at creation time, not per request.
- The `RANDOM` strategy below picks **which variant** to return. It does not change the generated data inside a variant.
- For values that change on every request (fresh IDs, timestamps, echoing the request), use a [CEL expression](./cel-expressions.md).

---

## Creating variants

### From the UI

1. Open a mock server and go to the **Endpoints** tab.
2. Expand any operation.
3. Click **Add Variant** and fill in the name, status code, and response body.
4. For dynamic responses, switch to the **CEL** tab instead of **Static**.
5. Save. The variant is immediately active.

### From the API

```bash
curl -X POST http://localhost:8080/mock-server/servers/{mockServerId}/variants \
  -H 'Content-Type: application/json' \
  -d '{
    "operationId": "getUser",
    "responseName": "Success",
    "statusCode": "200",
    "responseBody": "{\"id\": \"123\", \"name\": \"Alice\"}",
    "isDefault": true
  }'
```

---

## Response strategies

The response strategy controls **which variant is selected** when a request arrives. It is configured per mock server and can be overridden per operation.

### Available strategies

| Strategy | Behavior | Best for |
|----------|----------|----------|
| `RANDOM` *(default)* | Picks a variant at random on every request | Verifying your app handles any response correctly |
| `DEFAULT_ONLY` | Always returns the variant marked `isDefault: true` | Stable happy-path testing |
| `SEQUENTIAL` | Cycles through variants in display order, wrapping around | Scripting a specific sequence of responses |
| `ROUND_ROBIN` | Like `SEQUENTIAL` but position is **persisted** across restarts | Long-running or distributed test runs |

> [!NOTE]
> New mock servers default to `RANDOM`. To always get a predictable response, either switch to `DEFAULT_ONLY` or create a single variant.

### Setting the strategy

**From the UI:** Go to the **Configuration** tab and select a strategy from the dropdown.

**From the API:**

```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{mockServerId} \
  -H 'Content-Type: application/json' \
  -d '{"defaultStrategy": "SEQUENTIAL"}'
```

### Per-operation overrides

You can apply a different strategy to a specific operation without changing the server default:

```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{mockServerId}/operations/{operationId} \
  -H 'Content-Type: application/json' \
  -d '{"responseStrategy": "DEFAULT_ONLY"}'
```

---

## Forcing a specific status code

Any request can include the `X-spec0-Preferred-Response-Code` header to bypass the active strategy entirely and return the first variant that matches the requested status code:

```bash
curl http://localhost:8080/mock/{mockServerId}/users/123 \
  -H 'X-spec0-Preferred-Response-Code: 404'
```

> [!TIP]
> This is useful in tests where you need to trigger a specific error path on demand without reconfiguring the server between test cases.

---

## Managing variants

**Reordering:** Variants are served in `displayOrder` when using `SEQUENTIAL` or `ROUND_ROBIN`. Reorder them by dragging in the UI, or set `displayOrder` explicitly via the API.

**Deleting:** Click the trash icon in the UI, or:

```bash
curl -X DELETE http://localhost:8080/mock-server/servers/{mockServerId}/variants/{variantId}
```

**Reset to defaults:** The MCP tool `reset_to_defaults` removes all user-created variants and resets the strategy to `RANDOM`. See [MCP Integration](./mcp-integration.md).

---

## See also

- [CEL Expressions](./cel-expressions.md) — add dynamic logic to a variant
- [Schema Validation](./schema-validation.md) — validate variant bodies against your OpenAPI spec
- [Request Logs](./request-logs.md) — see which variant was selected for each request
- [← Documentation index](./README.md)
