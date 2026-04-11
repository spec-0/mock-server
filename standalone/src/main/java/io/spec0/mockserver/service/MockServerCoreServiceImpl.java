package io.spec0.mockserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.domain.*;
import io.spec0.mockserver.dto.MockServerExportDto;
import io.spec0.mockserver.dto.VariantCreateDto;
import io.spec0.mockserver.error.MockApiException;
import io.spec0.mockserver.mockgen.MockingClient;
import io.spec0.mockserver.openapi.validation.MockOpenApiValidator;
import io.spec0.mockserver.openapi.validation.OpenApiValidationResult;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import io.spec0.mockserver.port.ApiSpecServicePort;
import io.spec0.mockserver.port.MockServerServicePort;
import io.spec0.mockserver.repository.*;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MockServerCoreServiceImpl implements MockServerServicePort {

  private final MockServerRepository serverRepository;
  private final MockServerConfigRepository configRepository;
  private final MockVariantRepository variantRepository;
  private final MockOperationConfigRepository operationConfigRepository;
  private final MockRequestLogRepository logRepository;
  private final MockServerOperationRepository operationRepository;
  private final MockServerEnvVarRepository envVarRepository;
  private final ApiSpecServicePort apiSpecServicePort;
  private final MockOpenApiValidator mockOpenApiValidator;
  private final ObjectMapper objectMapper;

  // ── Lifecycle ────────────────────────────────────────────────────────────

  @Override
  public MockServerEntity createMockServer(
      UUID specId, String name, MockResponseStrategy strategy) {
    return createMockServerInternal(specId, name, strategy, null);
  }

  @Override
  public MockServerEntity createMockServerWithVariants(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> variants) {
    return createMockServerInternal(specId, name, strategy, variants);
  }

  private MockServerEntity createMockServerInternal(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> importedVariants) {
    ApiSpecEntity spec =
        apiSpecServicePort
            .findById(specId)
            .orElseThrow(() -> new EntityNotFoundException("Spec not found: " + specId));

    MockServerEntity server = new MockServerEntity(specId, name, strategy);
    MockServerEntity saved = serverRepository.save(server);

    configRepository.save(new MockServerConfigEntity(saved.getMockServerId()));

    List<MockServerOperationEntity> ops = operationRepository.findBySpecId(specId);
    for (MockServerOperationEntity op : ops) {
      operationConfigRepository.save(
          new MockOperationConfigEntity(saved.getMockServerId(), op.getOperationId()));
    }

    if (importedVariants != null && !importedVariants.isEmpty()) {
      for (VariantCreateDto dto : importedVariants) {
        persistVariant(saved.getMockServerId(), dto);
      }
    } else {
      generateDefaultVariants(saved.getMockServerId(), spec.getSpecContent(), ops);
    }

    log.info(
        "Created mock server '{}' (id={}) for spec '{}'",
        name,
        saved.getMockServerId(),
        spec.getSpecName());
    return saved;
  }

  private void generateDefaultVariants(
      UUID mockServerId, String specContent, List<MockServerOperationEntity> ops) {
    // MockingClient only understands JSON — convert YAML spec to JSON first via swagger-parser
    String specJson = toJson(specContent);
    // MockingClient returns { operationId: { statusCode: responseBody } }
    JsonNode allMocks = null;
    try {
      String mocksJson = new MockingClient().generateMockFromString(specJson);
      allMocks = objectMapper.readTree(mocksJson);
    } catch (JsonProcessingException e) {
      log.warn("Could not parse mock generation output: {}", e.getMessage());
    }

    for (MockServerOperationEntity op : ops) {
      try {
        String body = null;
        if (allMocks != null && allMocks.has(op.getOperationId())) {
          JsonNode opMocks = allMocks.get(op.getOperationId());
          // Prefer the success status code, fall back to first available
          JsonNode statusNode = opMocks.get(op.getSuccessStatusCode());
          if (statusNode == null) statusNode = opMocks.fields().next().getValue();
          if (statusNode != null) body = objectMapper.writeValueAsString(statusNode);
        }

        MockResponseVariantEntity v =
            new MockResponseVariantEntity(
                mockServerId,
                op.getOperationId(),
                "Default " + op.getOperationId(),
                op.getSuccessStatusCode(),
                body);
        v.setIsDefault(true);
        v.setIsGenerated(true);
        variantRepository.save(v);
      } catch (Exception e) {
        log.warn(
            "Could not generate default variant for operation {}: {}",
            op.getOperationId(),
            e.getMessage());
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockServerEntity> findById(UUID mockServerId) {
    return serverRepository.findById(mockServerId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockServerEntity> findAll() {
    return serverRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockServerEntity> findBySpecId(UUID specId) {
    return serverRepository.findBySpecId(specId);
  }

  @Override
  public void deleteMockServer(UUID mockServerId) {
    logRepository.deleteByMockServerId(mockServerId);
    variantRepository.deleteByMockServerId(mockServerId);
    operationConfigRepository.deleteByMockServerId(mockServerId);
    envVarRepository.deleteByMockServerId(mockServerId);
    configRepository
        .findByMockServerId(mockServerId)
        .ifPresent(c -> configRepository.deleteById(c.getConfigId()));
    serverRepository.deleteById(mockServerId);
    log.info("Deleted mock server {}", mockServerId);
  }

  // ── Variant management ───────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<MockResponseVariantEntity> getVariants(UUID mockServerId) {
    return variantRepository.findByMockServerIdOrderByDisplayOrder(mockServerId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockResponseVariantEntity> getVariantsByOperationId(
      UUID mockServerId, String operationId) {
    return variantRepository.findByMockServerIdAndOperationIdOrderByDisplayOrder(
        mockServerId, operationId);
  }

  @Override
  public MockResponseVariantEntity createVariant(UUID mockServerId, VariantCreateDto dto) {
    MockServerConfigEntity config =
        configRepository
            .findByMockServerId(mockServerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Mock server config not found: " + mockServerId));

    long total = variantRepository.countByMockServerId(mockServerId);
    long perOp = variantRepository.countByMockServerIdAndOperationId(mockServerId, dto.getOperationId());

    if (total >= config.getMaxTotalVariants()) {
      throw new IllegalArgumentException("Maximum total variants limit reached");
    }
    if (perOp >= config.getMaxVariantsPerOperation()) {
      throw new IllegalArgumentException("Maximum variants per operation limit reached");
    }

    return persistVariant(mockServerId, dto);
  }

  @Override
  public MockResponseVariantEntity updateVariant(
      UUID mockServerId, UUID variantId, VariantCreateDto dto) {
    MockResponseVariantEntity variant =
        variantRepository
            .findById(variantId)
            .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + variantId));

    if (!variant.getMockServerId().equals(mockServerId)) {
      throw new IllegalArgumentException("Variant does not belong to mock server " + mockServerId);
    }

    if (dto.isDefault() && !Boolean.TRUE.equals(variant.getIsDefault())) {
      variantRepository
          .findFirstByMockServerIdAndOperationIdAndIsDefaultTrue(mockServerId, variant.getOperationId())
          .ifPresent(
              existing -> {
                existing.setIsDefault(false);
                variantRepository.save(existing);
              });
    }

    variant.setResponseName(dto.getResponseName());
    variant.setStatusCode(dto.getStatusCode());
    variant.setResponseBody(dto.getResponseBody());
    variant.setHeaders(dto.getHeaders());
    variant.setIsDefault(dto.isDefault());
    if (dto.getDisplayOrder() != null) {
      variant.setDisplayOrder(dto.getDisplayOrder());
    }
    variant.setCelExpression(dto.getCelExpression());
    variant.setIsGenerated(false);
    MockServerEntity srv =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));
    validateStaticVariantResponse(mockServerId, srv.getSpecId(), dto);
    return variantRepository.save(variant);
  }

  @Override
  public void deleteVariant(UUID mockServerId, UUID variantId) {
    MockResponseVariantEntity variant =
        variantRepository
            .findById(variantId)
            .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + variantId));
    if (!variant.getMockServerId().equals(mockServerId)) {
      throw new IllegalArgumentException("Variant does not belong to mock server " + mockServerId);
    }
    variantRepository.deleteById(variantId);
  }

  // ── Export ───────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MockServerExportDto exportMockServer(UUID mockServerId) {
    MockServerEntity server =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));

    ApiSpecEntity spec =
        apiSpecServicePort
            .findById(server.getSpecId())
            .orElseThrow(() -> new EntityNotFoundException("Spec not found: " + server.getSpecId()));

    List<MockResponseVariantEntity> variants =
        variantRepository.findByMockServerIdOrderByDisplayOrder(mockServerId);

    List<MockServerExportDto.VariantSnapshot> variantSnapshots =
        variants.stream()
            .map(
                v ->
                    new MockServerExportDto.VariantSnapshot(
                        v.getOperationId(),
                        v.getResponseName(),
                        v.getStatusCode(),
                        v.getResponseBody(),
                        v.getHeaders(),
                        Boolean.TRUE.equals(v.getIsDefault()),
                        v.getDisplayOrder())
                )
            .toList();

    return new MockServerExportDto(
        "1.0",
        new MockServerExportDto.SpecSnapshot(
            spec.getSpecName(), spec.getSpecContent(), spec.getSpecHash(), spec.getSpecVersion()),
        new MockServerExportDto.ServerSnapshot(server.getName(), server.getDefaultStrategy()),
        variantSnapshots);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private MockResponseVariantEntity persistVariant(UUID mockServerId, VariantCreateDto dto) {
    if (dto.isDefault()) {
      variantRepository
          .findFirstByMockServerIdAndOperationIdAndIsDefaultTrue(mockServerId, dto.getOperationId())
          .ifPresent(
              existing -> {
                existing.setIsDefault(false);
                variantRepository.save(existing);
              });
    }

    MockResponseVariantEntity v =
        new MockResponseVariantEntity(
            mockServerId,
            dto.getOperationId(),
            dto.getResponseName(),
            dto.getStatusCode(),
            dto.getResponseBody());
    v.setHeaders(dto.getHeaders());
    v.setIsDefault(dto.isDefault());
    v.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
    v.setCelExpression(dto.getCelExpression());
    MockServerEntity srv =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));
    validateStaticVariantResponse(mockServerId, srv.getSpecId(), dto);
    return variantRepository.save(v);
  }

  private void validateStaticVariantResponse(
      UUID mockServerId, UUID specId, VariantCreateDto dto) {
    MockServerConfigEntity cfg = configRepository.findByMockServerId(mockServerId).orElse(null);
    if (cfg == null || cfg.getSchemaValidationMode() == SchemaValidationMode.OFF) {
      log.trace(
          "staticVariantValidation skipped: no_config_or_off mockServerId={} operationId={}",
          mockServerId,
          dto.getOperationId());
      return;
    }
    if (dto.getCelExpression() != null && !dto.getCelExpression().isBlank()) {
      log.trace(
          "staticVariantValidation skipped: cel_variant mockServerId={} operationId={}",
          mockServerId,
          dto.getOperationId());
      return;
    }
    log.trace(
        "staticVariantValidation start mockServerId={} specId={} operationId={} statusCode={} mode={} responseBodyChars={}",
        mockServerId,
        specId,
        dto.getOperationId(),
        dto.getStatusCode(),
        cfg.getSchemaValidationMode(),
        dto.getResponseBody() == null ? 0 : dto.getResponseBody().length());
    JsonNode bodyNode;
    try {
      String raw = dto.getResponseBody();
      if (raw == null || raw.isBlank()) {
        bodyNode = objectMapper.nullNode();
      } else {
        bodyNode = objectMapper.readTree(raw);
      }
    } catch (Exception e) {
      if (cfg.getSchemaValidationMode() == SchemaValidationMode.STRICT) {
        List<String> detail = new ArrayList<>();
        detail.add(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        log.trace(
            "staticVariantValidation outcome=invalid_json_strict operationId={} statusCode={} detail={}",
            dto.getOperationId(),
            dto.getStatusCode(),
            e.getMessage());
        throw MockApiException.badRequest(
            "invalid_json",
            String.format(
                "Response body must be valid JSON when schema validation is STRICT (operation \"%s\", HTTP status %s).",
                dto.getOperationId(),
                dto.getStatusCode()),
            detail);
      }
      log.warn(
          "Static variant response body is not valid JSON (WARN, operation {}): {}",
          dto.getOperationId(),
          e.getMessage());
      return;
    }
    OpenApiValidationResult r =
        mockOpenApiValidator.validateResponseBody(
            specId, dto.getOperationId(), dto.getStatusCode(), bodyNode, true);
    if (r.skipped()) {
      String why = r.skipReason() != null ? r.skipReason() : "unknown";
      log.trace(
          "staticVariantValidation outcome=skipped skipReason={} operationId={} statusCode={} mode={}",
          why,
          dto.getOperationId(),
          dto.getStatusCode(),
          cfg.getSchemaValidationMode());
      if (cfg.getSchemaValidationMode() == SchemaValidationMode.STRICT) {
        throw MockApiException.badRequest(
            "response_validation_unavailable",
            String.format(
                "Cannot validate this static variant under STRICT: operation \"%s\" / HTTP %s has no applicable application/json schema in the spec (%s). Fix the status code or OpenAPI response, or set validation to WARN/OFF.",
                dto.getOperationId(),
                dto.getStatusCode(),
                why),
            List.of(why));
      }
      log.warn(
          "Static variant response validation skipped (operation {} status {}): {}",
          dto.getOperationId(),
          dto.getStatusCode(),
          why);
      return;
    }
    if (r.valid()) {
      log.trace(
          "staticVariantValidation outcome=ok operationId={} statusCode={}",
          dto.getOperationId(),
          dto.getStatusCode());
      return;
    }
    if (cfg.getSchemaValidationMode() == SchemaValidationMode.WARN) {
      log.trace(
          "staticVariantValidation outcome=schema_mismatch_warn errorCount={} operationId={} errors={}",
          r.errors().size(),
          dto.getOperationId(),
          r.errors());
      log.warn("Static variant response validation (WARN): {}", r.errors());
      return;
    }
    log.trace(
        "staticVariantValidation outcome=schema_mismatch_strict errorCount={} operationId={} errors={}",
        r.errors().size(),
        dto.getOperationId(),
        r.errors());
    throw MockApiException.badRequest(
        "response_schema_validation_failed",
        String.format(
            "Mock response body does not match the OpenAPI JSON schema for operation \"%s\" and HTTP status %s (content type application/json).",
            dto.getOperationId(),
            dto.getStatusCode()),
        r.errors());
  }

  /**
   * Converts an OpenAPI spec (YAML or JSON) to JSON string. MockingClient only accepts JSON input.
   * Uses swagger-parser which handles both formats natively.
   */
  private String toJson(String specContent) {
    try {
      ParseOptions opts = new ParseOptions();
      opts.setResolve(false);
      OpenAPI api = new OpenAPIV3Parser().readContents(specContent, null, opts).getOpenAPI();
      if (api != null) {
        return Json.mapper().writeValueAsString(api);
      }
    } catch (Exception e) {
      log.warn("Could not convert spec to JSON for mock generation: {}", e.getMessage());
    }
    return specContent;
  }

  // ── Log helper (called by MockServerRequestHandler) ──────────────────────

  public void logRequest(
      UUID mockServerId,
      String operationId,
      String requestPath,
      String requestMethod,
      String responseStatus,
      UUID variantId) {
    MockRequestLogEntity log =
        new MockRequestLogEntity(
            mockServerId, operationId, requestPath, requestMethod, responseStatus, variantId);
    logRepository.save(log);
  }

  public List<MockRequestLogEntity> getRecentLogs(UUID mockServerId, int limit) {
    return logRepository.findByMockServerIdOrderByRequestedAtDesc(
        mockServerId, PageRequest.of(0, limit));
  }

  // ── MockServerServicePort extensions ──────────────────────────────────────

  @Override
  public MockServerEntity updateName(UUID mockServerId, String newName) {
    MockServerEntity server =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));
    server.setName(newName);
    return serverRepository.save(server);
  }

  @Override
  public void setEnabled(UUID mockServerId, boolean enabled) {
    MockServerEntity server =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));
    server.setIsEnabled(enabled);
    serverRepository.save(server);
  }

  @Override
  public MockServerEntity updateStrategy(UUID mockServerId, MockResponseStrategy strategy) {
    MockServerEntity server =
        serverRepository
            .findById(mockServerId)
            .orElseThrow(() -> new EntityNotFoundException("Mock server not found: " + mockServerId));
    server.setDefaultStrategy(strategy);
    return serverRepository.save(server);
  }

  @Override
  public void clearLogs(UUID mockServerId) {
    logRepository.deleteByMockServerId(mockServerId);
  }

  @Override
  public List<MockOperationConfigEntity> getOperationConfigs(UUID mockServerId) {
    return operationConfigRepository.findByMockServerId(mockServerId);
  }

  @Override
  public MockOperationConfigEntity updateOperationConfig(
      UUID mockServerId, String operationId, Boolean enabled, MockResponseStrategy strategyOverride) {
    MockOperationConfigEntity config =
        operationConfigRepository
            .findByMockServerIdAndOperationId(mockServerId, operationId)
            .orElseGet(() -> new MockOperationConfigEntity(mockServerId, operationId));
    if (enabled != null) config.setIsEnabled(enabled);
    config.setStrategyOverride(strategyOverride);
    return operationConfigRepository.save(config);
  }

  @Override
  public Optional<MockServerConfigEntity> getMockServerConfig(UUID mockServerId) {
    return configRepository.findByMockServerId(mockServerId);
  }
}
