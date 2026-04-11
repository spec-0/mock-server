# Schema Validation

The mock server can validate variant response bodies against your OpenAPI spec before saving them. This catches mismatches between the spec and the variants you configure, so your mocks stay accurate.

---

## Validation modes

| Mode | Behavior |
|------|----------|
| `OFF` *(default)* | No validation. Any JSON body is accepted. |
| `WARN` | The variant is saved. Violations are logged and returned in the `validationWarnings` field of the API response. |
| `STRICT` | The variant is **rejected** with `400 Bad Request` if the body violates the response schema. |

---

## Setting the mode

**From the UI:** Go to **Configuration** tab → **Schema Validation** → select a mode.

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

`validationWarnings` is absent when there are no violations.

---

## What gets validated

- **Static variant bodies** are validated against the OpenAPI response schema for the declared status code at **save time** (when you call `POST` or `PUT` on a variant).
- **CEL variants** are not validated at save time — their output is dynamic and only known at request time.
- **Incoming request bodies** are validated against the OpenAPI request schema at **request time** (when a mock request arrives), according to the same mode setting.

---

## Notes

- The default mode is `OFF` to avoid breaking existing setups when you first enable validation.
- Changing the mode does not retroactively validate existing variants — it only applies to new saves and incoming requests.
- Schema validation uses JSON Schema draft-07 semantics as interpreted by your OpenAPI spec version (3.0 or 3.1).
