# Schema Validation

The mock server can validate variant response bodies against your OpenAPI spec before saving them. This catches mismatches between the spec and the variants you configure, so your mocks stay accurate.

---

## Validation modes

| Mode | Behavior |
|------|----------|
| `OFF` *(default)* | No validation. Any JSON body is accepted. |
| `WARN` | The variant is saved. Violations are returned in `validationWarnings` and logged server-side. |
| `STRICT` | The variant is **rejected** with `400 Bad Request` if the body violates the response schema. |

> [!NOTE]
> The default mode is `OFF` so that existing setups are not affected when you first enable the feature. Switch to `WARN` to get visibility into violations without blocking saves, or `STRICT` to enforce correctness.

---

## Setting the mode

**From the UI:** Go to the **Configuration** tab → **Schema Validation** → select a mode.

**From the API:**

```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{mockServerId}/config \
  -H 'Content-Type: application/json' \
  -d '{"schemaValidationMode": "STRICT"}'
```

Valid values: `"OFF"`, `"WARN"`, `"STRICT"`.

---

## Warnings in WARN mode

When a variant is saved in `WARN` mode and the body has violations, the `201 Created` response includes a `validationWarnings` array:

```json
{
  "variantId": "a1b2c3...",
  "operationId": "createUser",
  "responseName": "Success",
  "statusCode": "201",
  "validationWarnings": [
    "$.email: is missing but it is required",
    "$.age: integer found, string expected"
  ]
}
```

`validationWarnings` is omitted entirely when there are no violations.

---

## What gets validated

| What | When |
|------|------|
| **Static variant bodies** | At **save time** — validated against the OpenAPI response schema for the declared status code |
| **CEL variant bodies** | Not validated at save time (output is only known at request time) |
| **Incoming request bodies** | At **request time** — validated against the OpenAPI request schema, per the same mode setting |

> [!NOTE]
> Changing the mode does not retroactively re-validate existing variants — it only applies to new saves and incoming requests going forward.

---

## See also

- [Variants & Response Strategies](./variants-and-strategies.md) — creating and managing variants
- [CEL Expressions](./cel-expressions.md) — dynamic variants and their relationship to validation
- [← Documentation index](./README.md)
