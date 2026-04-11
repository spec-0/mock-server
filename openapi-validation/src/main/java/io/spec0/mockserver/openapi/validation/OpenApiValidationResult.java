package io.spec0.mockserver.openapi.validation;

import java.util.List;

/**
 * Outcome of validating a JSON instance against an OpenAPI-derived JSON Schema.
 *
 * <p>{@link #skipped} results use {@code valid == false} so callers must not treat {@link
 * #valid()} alone as “passed validation” — check {@link #skipped()} first when enforcing STRICT
 * policies.
 */
public record OpenApiValidationResult(
    boolean valid, boolean skipped, String skipReason, List<String> errors) {

  public static OpenApiValidationResult ok() {
    return new OpenApiValidationResult(true, false, null, List.of());
  }

  public static OpenApiValidationResult failure(List<String> errors) {
    return new OpenApiValidationResult(false, false, null, List.copyOf(errors));
  }

  public static OpenApiValidationResult skipped(String reason) {
    return new OpenApiValidationResult(false, true, reason, List.of());
  }
}
