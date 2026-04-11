package io.spec0.mockserver.standalone.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.domain.MockResponseStrategy;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import io.spec0.mockserver.dto.VariantCreateDto;
import io.spec0.mockserver.port.MockServerServicePort;
import io.spec0.mockserver.repository.MockServerOperationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tools for controlling spec0 mock servers from AI coding assistants
 * (Claude Code, Cursor, etc.). Component-scanned automatically by
 * StandaloneMockServerApplication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockServerMcpTools {

  private final MockServerServicePort mockServerService;
  private final MockServerOperationRepository operationRepository;
  private final ObjectMapper objectMapper;

  @Tool(name = "list_mock_servers", description = "List all mock servers.")
  public String listMockServers() {
    return toJson(mockServerService.findAll());
  }

  @Tool(
      name = "list_operations",
      description =
          "List operations (endpoints) parsed from the OpenAPI spec for a mock server. "
              + "Returns operationId, HTTP method, path, and success status code. "
              + "Call this before create_cel_variant to understand available path params and methods.")
  public String listOperations(
      @ToolParam(description = "UUID of the mock server") String mockServerId) {
    var server =
        mockServerService
            .findById(UUID.fromString(mockServerId))
            .orElseThrow(() -> new IllegalArgumentException("Mock server not found: " + mockServerId));
    return toJson(operationRepository.findBySpecId(server.getSpecId()));
  }

  @Tool(
      name = "list_variants",
      description = "List response variants for a mock server, optionally filtered by operationId.")
  public String listVariants(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "Filter by operationId (optional, pass null to list all)", required = false)
          String operationId) {
    UUID id = UUID.fromString(mockServerId);
    List<MockResponseVariantEntity> variants =
        operationId != null && !operationId.isBlank()
            ? mockServerService.getVariantsByOperationId(id, operationId)
            : mockServerService.getVariants(id);
    return toJson(variants);
  }

  @Tool(
      name = "create_variant",
      description = "Create a static response variant for a mock server operation.")
  public String createVariant(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "Target operationId") String operationId,
      @ToolParam(description = "Display name for the variant") String name,
      @ToolParam(description = "HTTP status code, e.g. '200'") String statusCode,
      @ToolParam(description = "JSON response body as a string") String body,
      @ToolParam(description = "Whether this is the default variant") boolean isDefault) {
    VariantCreateDto dto = new VariantCreateDto();
    dto.setOperationId(operationId);
    dto.setResponseName(name);
    dto.setStatusCode(statusCode);
    dto.setResponseBody(body);
    dto.setDefault(isDefault);
    return toJson(mockServerService.createVariant(UUID.fromString(mockServerId), dto));
  }

  @Tool(
      name = "create_cel_variant",
      description =
          """
          Create a CEL (Common Expression Language) variant that generates dynamic responses at request time.

          CEL is Turing-incomplete and sandboxed — safe for server-side evaluation.

          Context variables available in the expression:
            request.method         — HTTP method string ("GET", "POST", etc.)
            request.path           — request path string ("/users/123")
            request.path_params    — map of path parameters  e.g. request.path_params.id
            request.query_params   — map of query parameters e.g. request.query_params.filter
            request.headers        — map of request headers (keys are lowercased, e.g. Session-Id → session-id)
            request.body           — parsed request body (map) or null

            env.<KEY>              — per-server env vars (managed via /env-vars endpoints)

          Built-in functions:
            uuid()                 — generates a random UUID string
            now()                  — current timestamp as ISO 8601 string
            randomInt(min, max)    — random integer in [min, max)

          Expression must return a map with keys 'status' (int), optional 'body' (any), optional 'headers' (map).

          IMPORTANT — CEL map keys:
            Use single-quoted string keys for maps. An unquoted identifier as a key (e.g. session_id) is a VARIABLE reference, not a string key, and will fail at runtime if that variable does not exist.
            Good:  {'status': 200, 'body': {'session_id': request.headers['session-id']}}
            Bad:   {"body": {session_id: ...}}  // session_id is read as a variable

          Example (return 404 for IDs starting with "99"):
            request.path_params.id.startsWith("99")
              ? {'status': 404, 'body': {'error': 'not found', 'id': request.path_params.id}}
              : {'status': 200, 'body': {'id': request.path_params.id, 'name': 'Jane Doe'}}

          Tip: call list_operations first to see available operationIds and their path templates.
          """)
  public String createCelVariant(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "Target operationId") String operationId,
      @ToolParam(description = "Display name for the variant") String name,
      @ToolParam(description = "Fallback HTTP status code if CEL eval fails, e.g. '200'")
          String statusCode,
      @ToolParam(description = "CEL expression (see description for syntax)") String celExpression) {
    VariantCreateDto dto = new VariantCreateDto();
    dto.setOperationId(operationId);
    dto.setResponseName(name);
    dto.setStatusCode(statusCode);
    dto.setResponseBody("{}");
    dto.setCelExpression(celExpression);
    return toJson(mockServerService.createVariant(UUID.fromString(mockServerId), dto));
  }

  @Tool(name = "delete_variant", description = "Delete a response variant.")
  public String deleteVariant(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "UUID of the variant to delete") String variantId) {
    mockServerService.deleteVariant(UUID.fromString(mockServerId), UUID.fromString(variantId));
    return "{\"deleted\": true}";
  }

  @Tool(
      name = "set_strategy",
      description =
          "Set the response selection strategy for a mock server. "
              + "Strategies: RANDOM, DEFAULT_ONLY, SEQUENTIAL, ROUND_ROBIN.")
  public String setStrategy(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "RANDOM | DEFAULT_ONLY | SEQUENTIAL | ROUND_ROBIN") String strategy) {
    MockResponseStrategy s = MockResponseStrategy.valueOf(strategy);
    return toJson(mockServerService.updateStrategy(UUID.fromString(mockServerId), s));
  }

  @Tool(name = "get_logs", description = "Get recent request logs for a mock server.")
  public String getLogs(
      @ToolParam(description = "UUID of the mock server") String mockServerId,
      @ToolParam(description = "Max entries to return (default 20, max 200)", required = false)
          Integer limit) {
    int n = limit != null ? Math.min(limit, 200) : 20;
    return toJson(mockServerService.getRecentLogs(UUID.fromString(mockServerId), n));
  }

  @Tool(
      name = "reset_to_defaults",
      description = "Delete all user-created variants and reset strategy to RANDOM.")
  public String resetToDefaults(
      @ToolParam(description = "UUID of the mock server") String mockServerId) {
    UUID id = UUID.fromString(mockServerId);
    mockServerService.getVariants(id).stream()
        .filter(v -> !Boolean.TRUE.equals(v.getIsGenerated()))
        .forEach(v -> mockServerService.deleteVariant(id, v.getVariantId()));
    mockServerService.updateStrategy(id, MockResponseStrategy.RANDOM);
    return "{\"reset\": true}";
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      log.warn("Failed to serialize MCP tool result", e);
      return "{}";
    }
  }
}
