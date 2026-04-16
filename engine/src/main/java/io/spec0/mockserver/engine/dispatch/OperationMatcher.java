package io.spec0.mockserver.engine.dispatch;

import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.openapi.OpenApiSpecSupport;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves an OpenAPI operation from HTTP method + path against registered {@link
 * MockServerOperation} rows. Pure Java — no persistence.
 */
public final class OperationMatcher {

  public record ResolvedOperation(String operationId, Map<String, Object> pathParams) {}

  private OperationMatcher() {}

  public static ResolvedOperation resolve(
      List<MockServerOperation> ops, String path, String method, String headerOverride) {
    if (headerOverride != null && !headerOverride.isBlank()) {
      return new ResolvedOperation(headerOverride, Map.of());
    }
    String m = method == null ? "" : method;

    for (MockServerOperation op : ops) {
      if (op.getPath().equals(path) && op.getHttpMethod().equalsIgnoreCase(m)) {
        return new ResolvedOperation(op.getOperationId(), Map.of());
      }
    }
    for (MockServerOperation op : ops) {
      if (op.getHttpMethod().equalsIgnoreCase(m)) {
        Map<String, Object> params = extractPathParams(op.getPath(), path);
        if (params != null) {
          return new ResolvedOperation(op.getOperationId(), params);
        }
      }
    }
    return new ResolvedOperation(OpenApiSpecSupport.synthesiseOperationId(m, path), Map.of());
  }

  /** Optional match — no synthetic fallback. */
  public static Optional<ResolvedOperation> resolveStrict(
      List<MockServerOperation> ops, String path, String method, String headerOverride) {
    if (headerOverride != null && !headerOverride.isBlank()) {
      return Optional.of(new ResolvedOperation(headerOverride, Map.of()));
    }
    String m = method == null ? "" : method;
    for (MockServerOperation op : ops) {
      if (op.getPath().equals(path) && op.getHttpMethod().equalsIgnoreCase(m)) {
        return Optional.of(new ResolvedOperation(op.getOperationId(), Map.of()));
      }
    }
    for (MockServerOperation op : ops) {
      if (op.getHttpMethod().equalsIgnoreCase(m)) {
        Map<String, Object> params = extractPathParams(op.getPath(), path);
        if (params != null) {
          return Optional.of(new ResolvedOperation(op.getOperationId(), params));
        }
      }
    }
    return Optional.empty();
  }

  static Map<String, Object> extractPathParams(String templatePath, String actualPath) {
    return PathTemplateUtil.extractPathParams(templatePath, actualPath);
  }
}
