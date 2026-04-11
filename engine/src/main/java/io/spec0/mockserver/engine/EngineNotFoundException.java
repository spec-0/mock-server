package io.spec0.mockserver.engine;

/** Non-JPA not-found signal for engine use cases (maps to HTTP 404 in adapters). */
public class EngineNotFoundException extends RuntimeException {

  public EngineNotFoundException(String message) {
    super(message);
  }
}
