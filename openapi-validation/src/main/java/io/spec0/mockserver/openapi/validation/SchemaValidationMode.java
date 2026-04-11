package io.spec0.mockserver.openapi.validation;

/** Policy for OpenAPI JSON Schema checks (request at runtime; static response body on save). */
public enum SchemaValidationMode {
  OFF,
  WARN,
  STRICT
}
