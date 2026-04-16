package io.spec0.mockserver.engine.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;

/**
 * Framework-agnostic mock response. Adapters convert this into their native response type (e.g.
 * {@code ResponseEntity<JsonNode>} in Spring).
 */
public record MockResponse(
    int statusCode,
    JsonNode body,
    Map<String, String> headers,
    String errorCode,
    UUID variantId,
    String resolvedOperationId) {

  /** Convenience: true if this response represents an error (no variant was selected). */
  public boolean isError() {
    return errorCode != null;
  }
}
