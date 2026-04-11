package io.spec0.mockserver.dto;

import java.util.List;

/**
 * Standard error envelope for management REST APIs so clients (including the UI) can show {@link
 * #message} and optional {@link #details} (e.g. JSON Schema validation lines).
 */
public record ApiErrorResponse(String error, String message, List<String> details) {

  public ApiErrorResponse {
    details = details == null ? List.of() : List.copyOf(details);
  }

  public static ApiErrorResponse of(String error, String message) {
    return new ApiErrorResponse(error, message, List.of());
  }
}
