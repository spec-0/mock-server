package io.spec0.mockserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.spec0.mockserver.domain.*;
import io.spec0.mockserver.openapi.validation.MockOpenApiValidator;
import io.spec0.mockserver.openapi.validation.OpenApiValidationResult;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import io.spec0.mockserver.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Core mock request routing and response selection engine. Ported from MockServerV2Controller +
 * MockServerVariantServiceImpl strategy logic — no platform dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockServerRequestHandler {

  private static final String HEADER_OPERATION_ID = "X-Mock-Operation-Id";
  private static final String HEADER_PREFERRED_STATUS = "X-spec0-Preferred-Response-Code";
  private static final String HEADER_MOCK_RESPONSE = "X-spec0-Mock-Response";
  private static final String HEADER_VARIANT_ID = "X-spec0-Mock-Variant-Id";
  private static final String HEADER_RESOLVED_OP = "X-spec0-Mock-Operation-Id";

  private final MockServerRepository serverRepository;
  private final MockServerOperationRepository operationRepository;
  private final MockVariantRepository variantRepository;
  private final MockOperationConfigRepository operationConfigRepository;
  private final MockServerEnvVarRepository envVarRepository;
  private final MockServerCoreServiceImpl coreService;
  private final CelExpressionEngine celEngine;
  private final MockOpenApiValidator mockOpenApiValidator;
  private final MockServerConfigRepository configRepository;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();

  @Transactional
  public ResponseEntity<JsonNode> handle(UUID mockServerId, HttpServletRequest request) {
    Optional<MockServerEntity> serverOpt = serverRepository.findById(mockServerId);
    if (serverOpt.isEmpty()) {
      return errorResponse(HttpStatus.NOT_FOUND, "mock_server_not_found", null);
    }

    MockServerEntity server = serverOpt.get();
    if (!Boolean.TRUE.equals(server.getIsEnabled())) {
      return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "mock_server_disabled", null);
    }

    String path = extractPath(request, mockServerId);
    String method = request.getMethod().toUpperCase();
    String operationIdOverride = request.getHeader(HEADER_OPERATION_ID);
    String preferredStatus = request.getHeader(HEADER_PREFERRED_STATUS);

    ResolvedOperation resolved =
        resolveOperation(server.getSpecId(), path, method, operationIdOverride);
    String operationId = resolved.operationId();
    log.info("Mock request {} {} → operationId={}", method, path, operationId);

    SchemaValidationMode validationMode =
        configRepository
            .findByMockServerId(mockServerId)
            .map(MockServerConfigEntity::getSchemaValidationMode)
            .orElse(SchemaValidationMode.OFF);
    if (validationMode != SchemaValidationMode.OFF) {
      ResponseEntity<JsonNode> validationError =
          validateIncomingRequest(server.getSpecId(), operationId, request, validationMode);
      if (validationError != null) {
        return validationError;
      }
    }

    Optional<MockOperationConfigEntity> opConfigOpt =
        operationConfigRepository.findByMockServerIdAndOperationId(mockServerId, operationId);

    if (opConfigOpt.isPresent() && !Boolean.TRUE.equals(opConfigOpt.get().getIsEnabled())) {
      return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "operation_disabled", operationId);
    }

    MockResponseStrategy strategy = resolveStrategy(server, opConfigOpt.orElse(null));
    List<MockResponseVariantEntity> variants =
        variantRepository.findByMockServerIdAndOperationIdOrderByDisplayOrder(
            mockServerId, operationId);

    if (variants.isEmpty()) {
      return errorResponse(HttpStatus.NOT_FOUND, "no_variants_for_operation", operationId);
    }

    MockResponseVariantEntity selected =
        selectVariant(variants, strategy, opConfigOpt, preferredStatus);

    // CEL evaluation — skip for static variants
    if (selected.getCelExpression() != null) {
      CelExpressionEngine.CelRequestContext ctx =
          buildCelContext(request, path, method, resolved.pathParams());
      Map<String, String> envVars = loadEnvVars(mockServerId);
      Optional<CelExpressionEngine.CelResult> celResult =
          celEngine.evaluate(selected.getCelExpression(), ctx, envVars);
      if (celResult.isPresent()) {
        coreService.logRequest(
            mockServerId,
            operationId,
            path,
            method,
            String.valueOf(celResult.get().status()),
            selected.getVariantId());
        return buildCelResponse(celResult.get(), selected, operationId);
      }
      // CEL eval failed — fall through to static responseBody
      log.warn(
          "CEL evaluation failed for variant {}, falling back to static body",
          selected.getVariantId());
    }

    coreService.logRequest(
        mockServerId, operationId, path, method, selected.getStatusCode(), selected.getVariantId());

    return buildResponse(selected, operationId);
  }

  // ── Operation resolution (two-pass: exact → regex → synthetic) ──────────

  record ResolvedOperation(String operationId, Map<String, Object> pathParams) {}

  ResolvedOperation resolveOperation(
      UUID specId, String path, String method, String headerOverride) {
    if (headerOverride != null && !headerOverride.isBlank()) {
      return new ResolvedOperation(headerOverride, Map.of());
    }

    List<MockServerOperationEntity> ops = operationRepository.findBySpecId(specId);

    // Pass 1: exact path + method match
    for (MockServerOperationEntity op : ops) {
      if (op.getPath().equals(path) && op.getHttpMethod().equalsIgnoreCase(method)) {
        return new ResolvedOperation(op.getOperationId(), Map.of());
      }
    }

    // Pass 2: path-variable regex match + extract params
    for (MockServerOperationEntity op : ops) {
      if (op.getHttpMethod().equalsIgnoreCase(method)) {
        Map<String, Object> params = extractPathParams(op.getPath(), path);
        if (params != null) {
          return new ResolvedOperation(op.getOperationId(), params);
        }
      }
    }

    // Fallback: synthetic
    return new ResolvedOperation(ApiSpecServiceImpl.synthesiseOperationId(method, path), Map.of());
  }

  /** Kept for backwards-compat with existing callers in tests. */
  String resolveOperationId(UUID specId, String path, String method, String headerOverride) {
    return resolveOperation(specId, path, method, headerOverride).operationId();
  }

  // ── Strategy selection ───────────────────────────────────────────────────

  private MockResponseStrategy resolveStrategy(
      MockServerEntity server, MockOperationConfigEntity opConfig) {
    if (opConfig != null && opConfig.getStrategyOverride() != null) {
      return opConfig.getStrategyOverride();
    }
    return server.getDefaultStrategy();
  }

  private MockResponseVariantEntity selectVariant(
      List<MockResponseVariantEntity> variants,
      MockResponseStrategy strategy,
      Optional<MockOperationConfigEntity> opConfigOpt,
      String preferredStatus) {

    // Preferred status code override
    if (preferredStatus != null && !preferredStatus.isBlank()) {
      String finalPreferred = preferredStatus;
      Optional<MockResponseVariantEntity> preferred =
          variants.stream()
              .filter(v -> v.getStatusCode().equals(finalPreferred))
              .min(java.util.Comparator.comparingInt(MockResponseVariantEntity::getDisplayOrder));
      if (preferred.isPresent()) return preferred.get();
    }

    return switch (strategy) {
      case RANDOM -> variants.get(random.nextInt(variants.size()));
      case DEFAULT_ONLY ->
          variants.stream()
              .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
              .findFirst()
              .orElse(variants.get(0));
      case SEQUENTIAL ->
          variants.stream()
              .min(java.util.Comparator.comparingInt(MockResponseVariantEntity::getDisplayOrder))
              .orElse(variants.get(0));
      case ROUND_ROBIN -> selectRoundRobin(variants, opConfigOpt.orElse(null));
    };
  }

  private MockResponseVariantEntity selectRoundRobin(
      List<MockResponseVariantEntity> variants, MockOperationConfigEntity opConfig) {
    if (opConfig == null) return variants.get(0);
    int pos = opConfig.getRoundRobinPosition() % variants.size();
    opConfig.setRoundRobinPosition(pos + 1);
    operationConfigRepository.save(opConfig);
    return variants.get(pos);
  }

  // ── Response building ────────────────────────────────────────────────────

  private ResponseEntity<JsonNode> buildResponse(
      MockResponseVariantEntity variant, String operationId) {
    int statusCode = parseStatusCode(variant.getStatusCode());
    JsonNode body = parseJsonBody(variant.getResponseBody());

    return ResponseEntity.status(statusCode)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HEADER_MOCK_RESPONSE, "true")
        .header(HEADER_VARIANT_ID, variant.getVariantId().toString())
        .header(HEADER_RESOLVED_OP, operationId)
        .body(body);
  }

  private ResponseEntity<JsonNode> buildCelResponse(
      CelExpressionEngine.CelResult result, MockResponseVariantEntity variant, String operationId) {

    JsonNode body =
        result.body() != null ? result.body() : parseJsonBody(variant.getResponseBody());

    ResponseEntity.BodyBuilder builder =
        ResponseEntity.status(result.status())
            .contentType(MediaType.APPLICATION_JSON)
            .header(HEADER_MOCK_RESPONSE, "true")
            .header(HEADER_VARIANT_ID, variant.getVariantId().toString())
            .header(HEADER_RESOLVED_OP, operationId);

    result.headers().forEach(builder::header);
    return builder.body(body);
  }

  private ResponseEntity<JsonNode> errorResponse(
      HttpStatus status, String errorCode, String operationId) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("error", errorCode);
    if (operationId != null) node.put("operationId", operationId);
    ResponseEntity.BodyBuilder builder =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HEADER_MOCK_RESPONSE, "true");
    if (operationId != null) builder.header(HEADER_RESOLVED_OP, operationId);
    return builder.body(node);
  }

  // ── CEL context helpers ──────────────────────────────────────────────────

  private CelExpressionEngine.CelRequestContext buildCelContext(
      HttpServletRequest request, String path, String method, Map<String, Object> pathParams) {

    // Query params
    Map<String, Object> queryParams = new HashMap<>();
    if (request.getQueryString() != null) {
      request.getParameterMap().forEach((k, v) -> queryParams.put(k, v.length == 1 ? v[0] : v));
    }

    // Request headers
    Map<String, Object> headers = new HashMap<>();
    java.util.Collections.list(request.getHeaderNames())
        .forEach(name -> headers.put(name.toLowerCase(), request.getHeader(name)));

    JsonNode body = readCachedJsonBody(request);

    return new CelExpressionEngine.CelRequestContext(
        method, path, pathParams, queryParams, headers, body);
  }

  private Map<String, String> loadEnvVars(UUID mockServerId) {
    return envVarRepository.findByMockServerId(mockServerId).stream()
        .collect(Collectors.toMap(e -> e.getVarKey(), e -> e.getVarValue()));
  }

  // ── Utilities ────────────────────────────────────────────────────────────

  private String extractPath(HttpServletRequest request, UUID mockServerId) {
    String uri = request.getRequestURI();
    String prefix = "/mock/" + mockServerId;
    if (uri.contains(prefix)) {
      String path = uri.substring(uri.indexOf(prefix) + prefix.length());
      return path.isEmpty() ? "/" : path;
    }
    return uri;
  }

  /**
   * Extracts path parameters by matching a path template against an actual path. Returns null if
   * the path doesn't match the template.
   *
   * <p>Example: template="/users/{id}", path="/users/123" → {"id": "123"}
   */
  private Map<String, Object> extractPathParams(String template, String path) {
    List<String> paramNames = new ArrayList<>();
    StringBuilder regexBuilder = new StringBuilder("^");
    Pattern varPattern = Pattern.compile("\\{([^/]+)}");
    Matcher templateMatcher = varPattern.matcher(template);
    int lastEnd = 0;

    while (templateMatcher.find()) {
      regexBuilder.append(Pattern.quote(template.substring(lastEnd, templateMatcher.start())));
      paramNames.add(templateMatcher.group(1));
      regexBuilder.append("([^/]+)");
      lastEnd = templateMatcher.end();
    }
    regexBuilder.append(Pattern.quote(template.substring(lastEnd)));
    regexBuilder.append("$");

    Matcher matcher = Pattern.compile(regexBuilder.toString()).matcher(path);
    if (!matcher.matches()) return null;

    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < paramNames.size(); i++) {
      params.put(paramNames.get(i), matcher.group(i + 1));
    }
    return params;
  }

  private int parseStatusCode(String code) {
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      return 200;
    }
  }

  private ResponseEntity<JsonNode> validateIncomingRequest(
      UUID specId, String operationId, HttpServletRequest request, SchemaValidationMode mode) {
    byte[] raw = readRawBody(request);
    String contentType = request.getContentType();
    log.trace(
        "mockRequestValidation start specId={} operationId={} mode={} rawBytes={} contentType={}",
        specId,
        operationId,
        mode,
        raw == null ? 0 : raw.length,
        contentType);
    if (raw != null && raw.length > 0 && isJsonContentType(request)) {
      try {
        objectMapper.readTree(raw);
      } catch (Exception e) {
        log.trace(
            "mockRequestValidation outcome=invalid_json operationId={} mode={} msg={}",
            operationId,
            mode,
            e.getMessage());
        if (mode == SchemaValidationMode.STRICT) {
          return invalidJsonResponse(operationId, e.getMessage());
        }
        log.warn("Request body is not valid JSON (WARN): {}", e.getMessage());
        return null;
      }
    }
    JsonNode bodyNode = readCachedJsonBody(request);
    boolean present = bodyNode != null && !bodyNode.isNull() && !bodyNode.isMissingNode();
    OpenApiValidationResult result =
        mockOpenApiValidator.validateRequestBody(specId, operationId, bodyNode, present);
    if (result.skipped()) {
      log.trace(
          "mockRequestValidation outcome=skipped operationId={} mode={} skipReason={}",
          operationId,
          mode,
          result.skipReason());
      return null;
    }
    if (result.valid()) {
      log.trace("mockRequestValidation outcome=ok operationId={} mode={}", operationId, mode);
      return null;
    }
    if (mode == SchemaValidationMode.WARN) {
      log.trace(
          "mockRequestValidation outcome=schema_mismatch_warn operationId={} errorCount={} errors={}",
          operationId,
          result.errors().size(),
          result.errors());
      log.warn("OpenAPI request validation (WARN): {}", result.errors());
      return null;
    }
    log.trace(
        "mockRequestValidation outcome=schema_mismatch_strict operationId={} errorCount={} errors={}",
        operationId,
        result.errors().size(),
        result.errors());
    return requestValidationFailedResponse(operationId, result.errors());
  }

  private byte[] readRawBody(HttpServletRequest request) {
    if (request instanceof ContentCachingRequestWrapper w) {
      return w.getContentAsByteArray();
    }
    return null;
  }

  private JsonNode readCachedJsonBody(HttpServletRequest request) {
    byte[] raw = readRawBody(request);
    if (raw == null || raw.length == 0) {
      return null;
    }
    try {
      return objectMapper.readTree(raw);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isJsonContentType(HttpServletRequest request) {
    String ct = request.getContentType();
    return ct != null && ct.toLowerCase().contains("json");
  }

  private ResponseEntity<JsonNode> invalidJsonResponse(String operationId, String parseError) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("error", "invalid_json");
    String why =
        parseError != null && !parseError.isBlank()
            ? parseError
            : "The body could not be parsed as JSON.";
    node.put(
        "message",
        String.format("Request body is not valid JSON (operation \"%s\"). %s", operationId, why));
    node.put("parseError", parseError != null ? parseError : "");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HEADER_MOCK_RESPONSE, "true")
        .body(node);
  }

  private ResponseEntity<JsonNode> requestValidationFailedResponse(
      String operationId, List<String> errors) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("error", "request_validation_failed");
    node.put(
        "message",
        String.format(
            "Request JSON body does not match the OpenAPI schema for operation \"%s\" (application/json).",
            operationId));
    var details = objectMapper.createArrayNode();
    for (String e : errors) {
      details.add(e);
    }
    node.set("details", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HEADER_MOCK_RESPONSE, "true")
        .body(node);
  }

  private JsonNode parseJsonBody(String body) {
    if (body == null || body.isBlank()) return objectMapper.createObjectNode();
    try {
      return objectMapper.readTree(body);
    } catch (Exception e) {
      ObjectNode node = objectMapper.createObjectNode();
      node.put("body", body);
      return node;
    }
  }
}
