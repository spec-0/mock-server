package io.spec0.mockserver.engine.service;

import io.spec0.mockserver.mockgen.MockingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.engine.EngineNotFoundException;
import io.spec0.mockserver.engine.api.MockServerServicePort;
import io.spec0.mockserver.engine.dto.MockServerExportDto;
import io.spec0.mockserver.engine.dto.VariantCreateDto;
import io.spec0.mockserver.engine.model.*;
import io.spec0.mockserver.engine.spi.ApiSpecLookup;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Framework-free mock server lifecycle and variant management. Adapters supply {@link
 * MockServerPersistencePort} and {@link ApiSpecLookup}; transaction boundaries are the adapter's
 * responsibility (e.g. Spring {@code @Transactional} on a delegating bean).
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultMockServerService implements MockServerServicePort {

  private final MockServerPersistencePort persistence;
  private final ApiSpecLookup apiSpecLookup;
  private final ObjectMapper objectMapper;

  public DefaultMockServerService(MockServerPersistencePort persistence, ApiSpecLookup apiSpecLookup) {
    this(persistence, apiSpecLookup, new ObjectMapper());
  }

  @Override
  public MockServer createMockServer(UUID specId, String name, MockResponseStrategy strategy) {
    return createMockServerInternal(specId, name, strategy, null);
  }

  @Override
  public MockServer createMockServerWithVariants(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> variants) {
    return createMockServerInternal(specId, name, strategy, variants);
  }

  private MockServer createMockServerInternal(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> importedVariants) {
    ApiSpecSnapshot spec =
        apiSpecLookup
            .findSpecById(specId)
            .orElseThrow(() -> new EngineNotFoundException("Spec not found: " + specId));

    MockServer server = new MockServer(specId, name, strategy);
    MockServer saved = persistence.saveMockServer(server);

    persistence.saveMockServerConfig(new MockServerConfig(saved.getMockServerId()));

    List<MockServerOperation> ops = persistence.findOperationsBySpecId(specId);
    for (MockServerOperation op : ops) {
      persistence.saveOperationConfig(new MockOperationConfig(saved.getMockServerId(), op.getOperationId()));
    }

    if (importedVariants != null && !importedVariants.isEmpty()) {
      for (VariantCreateDto dto : importedVariants) {
        persistVariant(saved.getMockServerId(), dto);
      }
    } else {
      generateDefaultVariants(saved.getMockServerId(), spec.specContent(), ops);
    }

    log.info(
        "Created mock server '{}' (id={}) for spec '{}'",
        name,
        saved.getMockServerId(),
        spec.specName());
    return saved;
  }

  private void generateDefaultVariants(
      UUID mockServerId, String specContent, List<MockServerOperation> ops) {
    String specJson = toJson(specContent);
    JsonNode allMocks = null;
    try {
      String mocksJson = new MockingClient().generateMockFromString(specJson);
      allMocks = objectMapper.readTree(mocksJson);
    } catch (JsonProcessingException e) {
      log.warn("Could not parse mock generation output: {}", e.getMessage());
    }

    for (MockServerOperation op : ops) {
      try {
        String body = null;
        if (allMocks != null && allMocks.has(op.getOperationId())) {
          JsonNode opMocks = allMocks.get(op.getOperationId());
          JsonNode statusNode = opMocks.get(op.getSuccessStatusCode());
          if (statusNode == null && opMocks.fields().hasNext()) {
            statusNode = opMocks.fields().next().getValue();
          }
          if (statusNode != null) {
            body = objectMapper.writeValueAsString(statusNode);
          }
        }

        MockResponseVariant v =
            new MockResponseVariant(
                mockServerId,
                op.getOperationId(),
                "Default " + op.getOperationId(),
                op.getSuccessStatusCode(),
                body);
        v.setIsDefault(true);
        v.setIsGenerated(true);
        persistence.saveVariant(v);
      } catch (Exception e) {
        log.warn(
            "Could not generate default variant for operation {}: {}",
            op.getOperationId(),
            e.getMessage());
      }
    }
  }

  @Override
  public Optional<MockServer> findById(UUID mockServerId) {
    return persistence.findMockServerById(mockServerId);
  }

  @Override
  public List<MockServer> findAll() {
    return persistence.findAllMockServers();
  }

  @Override
  public List<MockServer> findBySpecId(UUID specId) {
    return persistence.findMockServersBySpecId(specId);
  }

  @Override
  public void deleteMockServer(UUID mockServerId) {
    persistence.deleteLogsByMockServerId(mockServerId);
    persistence.deleteVariantsByMockServerId(mockServerId);
    persistence.deleteOperationConfigsByMockServerId(mockServerId);
    persistence.deleteEnvVarsByMockServerId(mockServerId);
    persistence
        .findConfigByMockServerId(mockServerId)
        .ifPresent(c -> persistence.deleteConfigById(c.getConfigId()));
    persistence.deleteMockServerById(mockServerId);
    log.info("Deleted mock server {}", mockServerId);
  }

  @Override
  public List<MockResponseVariant> getVariants(UUID mockServerId) {
    return persistence.findVariantsByMockServerIdOrderByDisplayOrder(mockServerId);
  }

  @Override
  public List<MockResponseVariant> getVariantsByOperationId(UUID mockServerId, String operationId) {
    return persistence.findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
        mockServerId, operationId);
  }

  @Override
  public MockResponseVariant createVariant(UUID mockServerId, VariantCreateDto dto) {
    MockServerConfig config =
        persistence
            .findConfigByMockServerId(mockServerId)
            .orElseThrow(
                () -> new EngineNotFoundException("Mock server config not found: " + mockServerId));

    long total = persistence.countVariantsByMockServerId(mockServerId);
    long perOp = persistence.countVariantsByMockServerIdAndOperationId(mockServerId, dto.getOperationId());

    if (total >= config.getMaxTotalVariants()) {
      throw new IllegalArgumentException("Maximum total variants limit reached");
    }
    if (perOp >= config.getMaxVariantsPerOperation()) {
      throw new IllegalArgumentException("Maximum variants per operation limit reached");
    }

    return persistVariant(mockServerId, dto);
  }

  @Override
  public MockResponseVariant updateVariant(UUID mockServerId, UUID variantId, VariantCreateDto dto) {
    MockResponseVariant variant =
        persistence
            .findVariantById(variantId)
            .orElseThrow(() -> new EngineNotFoundException("Variant not found: " + variantId));

    if (!variant.getMockServerId().equals(mockServerId)) {
      throw new IllegalArgumentException("Variant does not belong to mock server " + mockServerId);
    }

    if (dto.isDefault() && !Boolean.TRUE.equals(variant.getIsDefault())) {
      persistence
          .findFirstDefaultVariant(mockServerId, variant.getOperationId())
          .ifPresent(
              existing -> {
                existing.setIsDefault(false);
                persistence.saveVariant(existing);
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
    return persistence.saveVariant(variant);
  }

  @Override
  public void deleteVariant(UUID mockServerId, UUID variantId) {
    MockResponseVariant variant =
        persistence
            .findVariantById(variantId)
            .orElseThrow(() -> new EngineNotFoundException("Variant not found: " + variantId));
    if (!variant.getMockServerId().equals(mockServerId)) {
      throw new IllegalArgumentException("Variant does not belong to mock server " + mockServerId);
    }
    persistence.deleteVariantById(variantId);
  }

  @Override
  public MockServerExportDto exportMockServer(UUID mockServerId) {
    MockServer server =
        persistence
            .findMockServerById(mockServerId)
            .orElseThrow(() -> new EngineNotFoundException("Mock server not found: " + mockServerId));

    ApiSpecSnapshot spec =
        apiSpecLookup
            .findSpecById(server.getSpecId())
            .orElseThrow(() -> new EngineNotFoundException("Spec not found: " + server.getSpecId()));

    List<MockResponseVariant> variants =
        persistence.findVariantsByMockServerIdOrderByDisplayOrder(mockServerId);

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
                        v.getDisplayOrder() != null ? v.getDisplayOrder() : 0))
            .toList();

    return new MockServerExportDto(
        "1.0",
        new MockServerExportDto.SpecSnapshot(
            spec.specName(), spec.specContent(), spec.specHash(), spec.specVersion()),
        new MockServerExportDto.ServerSnapshot(server.getName(), server.getDefaultStrategy()),
        variantSnapshots);
  }

  private MockResponseVariant persistVariant(UUID mockServerId, VariantCreateDto dto) {
    if (dto.isDefault()) {
      persistence
          .findFirstDefaultVariant(mockServerId, dto.getOperationId())
          .ifPresent(
              existing -> {
                existing.setIsDefault(false);
                persistence.saveVariant(existing);
              });
    }

    MockResponseVariant v =
        new MockResponseVariant(
            mockServerId,
            dto.getOperationId(),
            dto.getResponseName(),
            dto.getStatusCode(),
            dto.getResponseBody());
    v.setHeaders(dto.getHeaders());
    v.setIsDefault(dto.isDefault());
    v.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
    v.setCelExpression(dto.getCelExpression());
    return persistence.saveVariant(v);
  }

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

  @Override
  public void logRequest(
      UUID mockServerId,
      String operationId,
      String requestPath,
      String requestMethod,
      String responseStatus,
      UUID variantId) {
    persistence.saveRequestLog(
        new MockRequestLog(
            mockServerId, operationId, requestPath, requestMethod, responseStatus, variantId));
  }

  @Override
  public List<MockRequestLog> getRecentLogs(UUID mockServerId, int limit) {
    return persistence.findRecentLogsByMockServerId(mockServerId, limit);
  }

  @Override
  public MockServer updateName(UUID mockServerId, String newName) {
    MockServer server =
        persistence
            .findMockServerById(mockServerId)
            .orElseThrow(() -> new EngineNotFoundException("Mock server not found: " + mockServerId));
    server.setName(newName);
    return persistence.saveMockServer(server);
  }

  @Override
  public void setEnabled(UUID mockServerId, boolean enabled) {
    MockServer server =
        persistence
            .findMockServerById(mockServerId)
            .orElseThrow(() -> new EngineNotFoundException("Mock server not found: " + mockServerId));
    server.setIsEnabled(enabled);
    persistence.saveMockServer(server);
  }

  @Override
  public MockServer updateStrategy(UUID mockServerId, MockResponseStrategy strategy) {
    MockServer server =
        persistence
            .findMockServerById(mockServerId)
            .orElseThrow(() -> new EngineNotFoundException("Mock server not found: " + mockServerId));
    server.setDefaultStrategy(strategy);
    return persistence.saveMockServer(server);
  }

  @Override
  public void clearLogs(UUID mockServerId) {
    persistence.deleteLogsByMockServerId(mockServerId);
  }

  @Override
  public List<MockOperationConfig> getOperationConfigs(UUID mockServerId) {
    return persistence.findOperationConfigsByMockServerId(mockServerId);
  }

  @Override
  public MockOperationConfig updateOperationConfig(
      UUID mockServerId, String operationId, Boolean enabled, MockResponseStrategy strategyOverride) {
    MockOperationConfig config =
        persistence
            .findOperationConfigByMockServerIdAndOperationId(mockServerId, operationId)
            .orElseGet(() -> new MockOperationConfig(mockServerId, operationId));
    if (enabled != null) {
      config.setIsEnabled(enabled);
    }
    config.setStrategyOverride(strategyOverride);
    return persistence.saveOperationConfig(config);
  }

  @Override
  public Optional<MockServerConfig> getMockServerConfig(UUID mockServerId) {
    return persistence.findConfigByMockServerId(mockServerId);
  }
}
