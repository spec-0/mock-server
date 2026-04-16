package io.spec0.mockserver.engine.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.spec0.mockserver.engine.model.MockOperationConfig;
import io.spec0.mockserver.engine.model.MockRequestLog;
import io.spec0.mockserver.engine.model.MockRequestLogMetric;
import io.spec0.mockserver.engine.model.MockResponseStrategy;
import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import io.spec0.mockserver.engine.model.MockServerConfig;
import io.spec0.mockserver.engine.spi.MockServerEnvVarPort;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import io.spec0.mockserver.openapi.validation.MockOpenApiValidator;
import io.spec0.mockserver.openapi.validation.OpenApiValidationResult;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pure-Java mock request routing and response selection engine.
 *
 * <p>No framework annotations — construct via {@code new MockRequestDispatcher(...)} and register
 * as a bean in your framework's configuration. Adapters translate their native request/response
 * types to/from {@link MockRequest} and {@link MockResponse}.
 */
public class MockRequestDispatcher {

  private static final Logger log = LoggerFactory.getLogger(MockRequestDispatcher.class);

  private static final String HEADER_MOCK_RESPONSE = "X-spec0-Mock-Response";
  private static final String HEADER_VARIANT_ID = "X-spec0-Mock-Variant-Id";
  private static final String HEADER_RESOLVED_OP = "X-spec0-Mock-Operation-Id";

  private final MockServerPersistencePort persistence;
  private final MockServerEnvVarPort envVarPort;
  private final CelEvaluator celEvaluator;
  private final MockOpenApiValidator validator;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();

  /**
   * @param persistence outbound persistence port
   * @param envVarPort outbound env-var port (used for CEL context)
   * @param celEvaluator CEL evaluator instance
   * @param validator OpenAPI schema validator; may be {@code null} to skip validation entirely
   * @param objectMapper shared Jackson mapper
   */
  public MockRequestDispatcher(
      MockServerPersistencePort persistence,
      MockServerEnvVarPort envVarPort,
      CelEvaluator celEvaluator,
      MockOpenApiValidator validator,
      ObjectMapper objectMapper) {
    this.persistence = persistence;
    this.envVarPort = envVarPort;
    this.celEvaluator = celEvaluator;
    this.validator = validator;
    this.objectMapper = objectMapper;
  }

  public MockResponse dispatch(UUID mockServerId, MockRequest request) {
    final long dispatchStartedNanos = System.nanoTime();
    Optional<MockServer> serverOpt = persistence.findMockServerById(mockServerId);
    if (serverOpt.isEmpty()) {
      return errorResponse(404, "mock_server_not_found", null);
    }

    MockServer server = serverOpt.get();
    if (!Boolean.TRUE.equals(server.getIsEnabled())) {
      return errorResponse(503, "mock_server_disabled", null);
    }

    String path = request.path();
    String method = request.method();
    List<io.spec0.mockserver.engine.model.MockServerOperation> operations =
        persistence.findOperationsBySpecId(server.getSpecId());

    OperationMatcher.ResolvedOperation resolved =
        OperationMatcher.resolve(operations, path, method, request.operationIdOverride());
    String operationId = resolved.operationId();
    log.info(
        "mockDispatch resolved mockServerId={} specId={} method={} path={} operationId={} pathParams={} operationsCount={} override={}",
        mockServerId,
        server.getSpecId(),
        method,
        path,
        operationId,
        resolved.pathParams(),
        operations.size(),
        request.operationIdOverride());

    SchemaValidationMode validationMode =
        persistence
            .findConfigByMockServerId(mockServerId)
            .map(MockServerConfig::getSchemaValidationMode)
            .orElse(SchemaValidationMode.OFF);

    if (validationMode != SchemaValidationMode.OFF && validator != null) {
      MockResponse validationError =
          validateIncomingRequest(server.getSpecId(), operationId, request, validationMode);
      if (validationError != null) {
        return validationError;
      }
    }

    Optional<MockOperationConfig> opConfigOpt =
        persistence.findOperationConfigByMockServerIdAndOperationId(mockServerId, operationId);

    if (opConfigOpt.isPresent() && !Boolean.TRUE.equals(opConfigOpt.get().getIsEnabled())) {
      return errorResponse(503, "operation_disabled", operationId);
    }

    MockResponseStrategy strategy = resolveStrategy(server, opConfigOpt.orElse(null));
    List<MockResponseVariant> variants =
        persistence.findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
            mockServerId, operationId);
    log.info(
        "mockDispatch variants mockServerId={} operationId={} strategy={} variantCount={} operationEnabled={}",
        mockServerId,
        operationId,
        strategy,
        variants.size(),
        opConfigOpt.map(MockOperationConfig::getIsEnabled).orElse(true));

    if (variants.isEmpty()) {
      return errorResponse(404, "no_variants_for_operation", operationId);
    }

    MockResponseVariant selected =
        selectVariant(variants, strategy, opConfigOpt, request.preferredStatusCode());
    log.info(
        "mockDispatch selected mockServerId={} operationId={} variantId={} statusCode={} responseName={} preferredStatus={}",
        mockServerId,
        operationId,
        selected.getVariantId(),
        selected.getStatusCode(),
        selected.getResponseName(),
        request.preferredStatusCode());

    // CEL evaluation
    if (selected.getCelExpression() != null) {
      CelEvaluator.CelRequestContext ctx = buildCelContext(request, resolved.pathParams());
      Map<String, String> envVars = envVarPort.findEnvVarsByMockServerId(mockServerId);
      Optional<CelEvaluator.CelResult> celResult =
          celEvaluator.evaluate(selected.getCelExpression(), ctx, envVars);
      if (celResult.isPresent()) {
        MockRequestLog celLog =
            new MockRequestLog(
                mockServerId,
                operationId,
                path,
                method,
                String.valueOf(celResult.get().status()),
                selected.getVariantId());
        appendDispatchLatency(celLog, dispatchStartedNanos);
        persistence.saveRequestLog(celLog);
        return buildCelResponse(celResult.get(), selected, operationId);
      }
      log.warn(
          "CEL evaluation failed for variant {}, falling back to static body",
          selected.getVariantId());
    }

    MockRequestLog okLog =
        new MockRequestLog(
            mockServerId,
            operationId,
            path,
            method,
            selected.getStatusCode(),
            selected.getVariantId());
    appendDispatchLatency(okLog, dispatchStartedNanos);
    persistence.saveRequestLog(okLog);

    return buildResponse(selected, operationId);
  }

  private static void appendDispatchLatency(MockRequestLog log, long dispatchStartedNanos) {
    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - dispatchStartedNanos);
    log.getMetrics().add(MockRequestLogMetric.latencyMs(Math.max(0, ms)));
  }

  // ── Operation resolution (see OperationMatcher) ─────────────────────────

  // ── Strategy selection ───────────────────────────────────────────────────

  private MockResponseStrategy resolveStrategy(MockServer server, MockOperationConfig opConfig) {
    if (opConfig != null && opConfig.getStrategyOverride() != null) {
      return opConfig.getStrategyOverride();
    }
    return server.getDefaultStrategy();
  }

  private MockResponseVariant selectVariant(
      List<MockResponseVariant> variants,
      MockResponseStrategy strategy,
      Optional<MockOperationConfig> opConfigOpt,
      String preferredStatus) {

    if (preferredStatus != null && !preferredStatus.isBlank()) {
      String finalPreferred = preferredStatus;
      Optional<MockResponseVariant> preferred =
          variants.stream()
              .filter(v -> v.getStatusCode().equals(finalPreferred))
              .min(java.util.Comparator.comparingInt(MockResponseVariant::getDisplayOrder));
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
              .min(java.util.Comparator.comparingInt(MockResponseVariant::getDisplayOrder))
              .orElse(variants.get(0));
      case ROUND_ROBIN -> selectRoundRobin(variants, opConfigOpt.orElse(null));
    };
  }

  private MockResponseVariant selectRoundRobin(
      List<MockResponseVariant> variants, MockOperationConfig opConfig) {
    if (opConfig == null) return variants.get(0);
    int pos = opConfig.getRoundRobinPosition() % variants.size();
    opConfig.setRoundRobinPosition(pos + 1);
    persistence.saveOperationConfig(opConfig);
    return variants.get(pos);
  }

  // ── Response building ────────────────────────────────────────────────────

  private MockResponse buildResponse(MockResponseVariant variant, String operationId) {
    int status = parseStatusCode(variant.getStatusCode());
    JsonNode body = parseJsonBody(variant.getResponseBody());
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER_MOCK_RESPONSE, "true");
    headers.put(HEADER_VARIANT_ID, variant.getVariantId().toString());
    headers.put(HEADER_RESOLVED_OP, operationId);
    return new MockResponse(status, body, headers, null, variant.getVariantId(), operationId);
  }

  private MockResponse buildCelResponse(
      CelEvaluator.CelResult result, MockResponseVariant variant, String operationId) {
    JsonNode body =
        result.body() != null ? result.body() : parseJsonBody(variant.getResponseBody());
    Map<String, String> headers = new HashMap<>(result.headers());
    headers.put(HEADER_MOCK_RESPONSE, "true");
    headers.put(HEADER_VARIANT_ID, variant.getVariantId().toString());
    headers.put(HEADER_RESOLVED_OP, operationId);
    return new MockResponse(
        result.status(), body, headers, null, variant.getVariantId(), operationId);
  }

  private MockResponse errorResponse(int status, String errorCode, String operationId) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("error", errorCode);
    if (operationId != null) node.put("operationId", operationId);
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER_MOCK_RESPONSE, "true");
    if (operationId != null) headers.put(HEADER_RESOLVED_OP, operationId);
    return new MockResponse(status, node, headers, errorCode, null, operationId);
  }

  // ── Validation ────────────────────────────────────────────────────────────

  private MockResponse validateIncomingRequest(
      UUID specId, String operationId, MockRequest request, SchemaValidationMode mode) {
    byte[] raw = request.rawBodyBytes();
    log.trace(
        "mockRequestValidation start specId={} operationId={} mode={} rawBytes={}",
        specId,
        operationId,
        mode,
        raw == null ? 0 : raw.length);

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

    JsonNode bodyNode = request.body();
    boolean present = bodyNode != null && !bodyNode.isNull() && !bodyNode.isMissingNode();
    OpenApiValidationResult result =
        validator.validateRequestBody(specId, operationId, bodyNode, present);

    if (result.skipped()) {
      log.trace(
          "mockRequestValidation outcome=skipped operationId={} skipReason={}",
          operationId,
          result.skipReason());
      return null;
    }
    if (result.valid()) {
      log.trace("mockRequestValidation outcome=ok operationId={}", operationId);
      return null;
    }
    if (mode == SchemaValidationMode.WARN) {
      log.warn("OpenAPI request validation (WARN): {}", result.errors());
      return null;
    }
    return requestValidationFailedResponse(operationId, result.errors());
  }

  private boolean isJsonContentType(MockRequest request) {
    String ct = request.headers().getOrDefault("content-type", "");
    return ct.toLowerCase().contains("json");
  }

  private MockResponse invalidJsonResponse(String operationId, String parseError) {
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
    Map<String, String> headers = Map.of(HEADER_MOCK_RESPONSE, "true");
    return new MockResponse(400, node, headers, "invalid_json", null, operationId);
  }

  private MockResponse requestValidationFailedResponse(String operationId, List<String> errors) {
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
    Map<String, String> headers = Map.of(HEADER_MOCK_RESPONSE, "true");
    return new MockResponse(400, node, headers, "request_validation_failed", null, operationId);
  }

  // ── Utilities ────────────────────────────────────────────────────────────

  private CelEvaluator.CelRequestContext buildCelContext(
      MockRequest request, Map<String, Object> pathParams) {
    Map<String, Object> queryParams = new HashMap<>(request.queryParams());
    Map<String, Object> headers = new HashMap<>(request.headers());
    return new CelEvaluator.CelRequestContext(
        request.method(), request.path(), pathParams, queryParams, headers, request.body());
  }

  private int parseStatusCode(String code) {
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      return 200;
    }
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
