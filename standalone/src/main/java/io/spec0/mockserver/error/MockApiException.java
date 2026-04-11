package io.spec0.mockserver.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Client-facing API failure with HTTP status, machine {@link #code}, and optional {@link #details}. */
public final class MockApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;
  private final List<String> details;

  public MockApiException(HttpStatus status, String code, String message, List<String> details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details == null ? List.of() : List.copyOf(details);
  }

  public static MockApiException badRequest(String code, String message, List<String> details) {
    return new MockApiException(HttpStatus.BAD_REQUEST, code, message, details);
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public List<String> getDetails() {
    return details;
  }
}
