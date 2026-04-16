package io.spec0.mockserver.engine.openapi;

import io.spec0.mockserver.engine.model.MockServerOperation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Pure OpenAPI parsing and hashing for mock-server — no persistence. Works with minimal valid
 * OpenAPI 3.x documents (paths + verb + responses with at least a status key where required).
 */
@Slf4j
public final class OpenApiSpecSupport {

  private OpenApiSpecSupport() {}

  public static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public static String parseInfoVersion(String specContent) {
    try {
      ParseOptions opts = new ParseOptions();
      opts.setResolve(false);
      SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, opts);
      if (result.getOpenAPI() != null && result.getOpenAPI().getInfo() != null) {
        return result.getOpenAPI().getInfo().getVersion();
      }
    } catch (Exception e) {
      log.warn("Could not parse spec version: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Extracts operations for a spec. Uses {@code resolve(true)} so internal {@code $ref}s resolve
   * when present; minimal specs without components still parse.
   */
  public static List<MockServerOperation> extractOperations(UUID specId, String specContent) {
    List<MockServerOperation> ops = new ArrayList<>();
    try {
      ParseOptions opts = new ParseOptions();
      opts.setResolve(true);
      SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, opts);
      OpenAPI openAPI = result.getOpenAPI();
      if (openAPI == null || openAPI.getPaths() == null) {
        log.warn("No paths found in spec {}", specId != null ? specId : "(new)");
        return ops;
      }
      for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
        String path = entry.getKey();
        PathItem item = entry.getValue();
        addIfPresent(ops, specId, "GET", path, item.getGet());
        addIfPresent(ops, specId, "POST", path, item.getPost());
        addIfPresent(ops, specId, "PUT", path, item.getPut());
        addIfPresent(ops, specId, "DELETE", path, item.getDelete());
        addIfPresent(ops, specId, "PATCH", path, item.getPatch());
      }
    } catch (Exception e) {
      log.error("Error parsing operations from spec {}: {}", specId, e.getMessage());
    }
    return ops;
  }

  private static void addIfPresent(
      List<MockServerOperation> ops, UUID specId, String method, String path, Operation operation) {
    if (operation == null) {
      return;
    }
    String operationId =
        operation.getOperationId() != null && !operation.getOperationId().isBlank()
            ? operation.getOperationId()
            : synthesiseOperationId(method, path);
    String successStatus = findFirstSuccessStatus(operation);
    ops.add(new MockServerOperation(specId, operationId, method, path, successStatus));
  }

  private static String findFirstSuccessStatus(Operation operation) {
    if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
      return "200";
    }
    return operation.getResponses().keySet().stream()
        .filter(code -> code != null && code.startsWith("2"))
        .findFirst()
        .orElse("200");
  }

  /** Same algorithm as platform / standalone catalog — must match mockgen {@code OpenApiParser}. */
  public static String synthesiseOperationId(String method, String path) {
    return method.toLowerCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
  }
}
