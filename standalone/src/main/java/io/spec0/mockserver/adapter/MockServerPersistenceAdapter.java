package io.spec0.mockserver.adapter;

import io.spec0.mockserver.domain.MockOperationConfigEntity;
import io.spec0.mockserver.domain.MockRequestLogEntity;
import io.spec0.mockserver.domain.MockRequestLogMetricEntity;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import io.spec0.mockserver.domain.MockServerConfigEntity;
import io.spec0.mockserver.domain.MockServerEntity;
import io.spec0.mockserver.domain.MockServerOperationEntity;
import io.spec0.mockserver.engine.model.ContentTypeConstants;
import io.spec0.mockserver.engine.model.MockOperationConfig;
import io.spec0.mockserver.engine.model.MockRequestLog;
import io.spec0.mockserver.engine.model.MockRequestLogMetric;
import io.spec0.mockserver.engine.model.MockResponseStrategy;
import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import io.spec0.mockserver.engine.model.MockServerConfig;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import io.spec0.mockserver.repository.MockOperationConfigRepository;
import io.spec0.mockserver.repository.MockRequestLogMetricRepository;
import io.spec0.mockserver.repository.MockRequestLogRepository;
import io.spec0.mockserver.repository.MockServerConfigRepository;
import io.spec0.mockserver.repository.MockServerOperationRepository;
import io.spec0.mockserver.repository.MockServerRepository;
import io.spec0.mockserver.repository.MockVariantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring/JPA implementation of {@link MockServerPersistencePort}. Translates between the engine's
 * pure POJO models and the standalone JPA entities.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class MockServerPersistenceAdapter implements MockServerPersistencePort {

  private final MockServerRepository serverRepository;
  private final MockVariantRepository variantRepository;
  private final MockOperationConfigRepository operationConfigRepository;
  private final MockServerConfigRepository configRepository;
  private final MockServerOperationRepository operationRepository;
  private final MockRequestLogRepository logRepository;
  private final MockRequestLogMetricRepository logMetricRepository;

  // ── MockServer ───────────────────────────────────────────────────────────

  @Override
  public MockServer saveMockServer(MockServer server) {
    MockServerEntity entity = toEntity(server);
    return fromEntity(serverRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockServer> findMockServerById(UUID mockServerId) {
    return serverRepository.findById(mockServerId).map(this::fromEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockServer> findAllMockServers() {
    return serverRepository.findAll().stream().map(this::fromEntity).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockServer> findMockServersBySpecId(UUID specId) {
    return serverRepository.findBySpecId(specId).stream().map(this::fromEntity).toList();
  }

  @Override
  public void deleteMockServerById(UUID mockServerId) {
    serverRepository.deleteById(mockServerId);
  }

  // ── MockServerConfig ─────────────────────────────────────────────────────

  @Override
  public MockServerConfig saveMockServerConfig(MockServerConfig config) {
    MockServerConfigEntity entity = toConfigEntity(config);
    return fromConfigEntity(configRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockServerConfig> findConfigByMockServerId(UUID mockServerId) {
    return configRepository.findByMockServerId(mockServerId).map(this::fromConfigEntity);
  }

  @Override
  public void deleteConfigById(UUID configId) {
    configRepository.deleteById(configId);
  }

  // ── MockResponseVariant ──────────────────────────────────────────────────

  @Override
  public MockResponseVariant saveVariant(MockResponseVariant variant) {
    MockResponseVariantEntity entity = toVariantEntity(variant);
    return fromVariantEntity(variantRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockResponseVariant> findVariantById(UUID variantId) {
    return variantRepository.findById(variantId).map(this::fromVariantEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockResponseVariant> findVariantsByMockServerIdOrderByDisplayOrder(
      UUID mockServerId) {
    return variantRepository.findByMockServerIdOrderByDisplayOrder(mockServerId).stream()
        .map(this::fromVariantEntity)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockResponseVariant> findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
      UUID mockServerId, String operationId) {
    return variantRepository
        .findByMockServerIdAndOperationIdOrderByDisplayOrder(mockServerId, operationId)
        .stream()
        .map(this::fromVariantEntity)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockResponseVariant> findFirstDefaultVariant(
      UUID mockServerId, String operationId) {
    return variantRepository
        .findFirstByMockServerIdAndOperationIdAndIsDefaultTrue(mockServerId, operationId)
        .map(this::fromVariantEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public long countVariantsByMockServerId(UUID mockServerId) {
    return variantRepository.countByMockServerId(mockServerId);
  }

  @Override
  @Transactional(readOnly = true)
  public long countVariantsByMockServerIdAndOperationId(UUID mockServerId, String operationId) {
    return variantRepository.countByMockServerIdAndOperationId(mockServerId, operationId);
  }

  @Override
  public void deleteVariantById(UUID variantId) {
    variantRepository.deleteById(variantId);
  }

  @Override
  public void deleteVariantsByMockServerId(UUID mockServerId) {
    variantRepository.deleteByMockServerId(mockServerId);
  }

  // ── MockOperationConfig ──────────────────────────────────────────────────

  @Override
  public MockOperationConfig saveOperationConfig(MockOperationConfig config) {
    MockOperationConfigEntity entity = toOpConfigEntity(config);
    return fromOpConfigEntity(operationConfigRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockOperationConfig> findOperationConfigsByMockServerId(UUID mockServerId) {
    return operationConfigRepository.findByMockServerId(mockServerId).stream()
        .map(this::fromOpConfigEntity)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MockOperationConfig> findOperationConfigByMockServerIdAndOperationId(
      UUID mockServerId, String operationId) {
    return operationConfigRepository
        .findByMockServerIdAndOperationId(mockServerId, operationId)
        .map(this::fromOpConfigEntity);
  }

  @Override
  public void deleteOperationConfigsByMockServerId(UUID mockServerId) {
    operationConfigRepository.deleteByMockServerId(mockServerId);
  }

  // ── MockRequestLog ───────────────────────────────────────────────────────

  @Override
  public void saveRequestLog(MockRequestLog log) {
    MockRequestLogEntity entity =
        new MockRequestLogEntity(
            log.getMockServerId(),
            log.getOperationId(),
            log.getRequestPath(),
            log.getRequestMethod(),
            log.getResponseStatusCode(),
            log.getVariantId());
    if (log.getRequestedAt() != null) {
      entity.setRequestedAt(log.getRequestedAt());
    }
    MockRequestLogEntity saved = logRepository.save(entity);
    if (log.getMetrics() != null) {
      for (MockRequestLogMetric m : log.getMetrics()) {
        logMetricRepository.save(
            new MockRequestLogMetricEntity(
                saved.getLogId(), m.key(), m.intValue(), m.doubleValue(), m.textValue()));
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<MockRequestLog> findRecentLogsByMockServerId(UUID mockServerId, int limit) {
    return logRepository
        .findByMockServerIdOrderByRequestedAtDesc(mockServerId, PageRequest.of(0, limit))
        .stream()
        .map(this::fromLogEntity)
        .toList();
  }

  @Override
  public void deleteLogsByMockServerId(UUID mockServerId) {
    logRepository.deleteByMockServerId(mockServerId);
  }

  // ── MockServerEnvVar ─────────────────────────────────────────────────────

  @Override
  public void deleteEnvVarsByMockServerId(UUID mockServerId) {
    // Env var deletion is handled via MockServerEnvVarRepository in the service layer
    // No direct method needed here; the engine port only declares this for cleanup
  }

  // ── MockServerOperation ──────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<MockServerOperation> findOperationsBySpecId(UUID specId) {
    return operationRepository.findBySpecId(specId).stream()
        .map(this::fromOperationEntity)
        .toList();
  }

  // ── Mapping: entity → POJO ───────────────────────────────────────────────

  private MockServer fromEntity(MockServerEntity e) {
    MockServer s = new MockServer();
    s.setMockServerId(e.getMockServerId());
    s.setSpecId(e.getSpecId());
    s.setName(e.getName());
    s.setApiKeyHash(e.getApiKeyHash());
    s.setApiKeyPreview(e.getApiKeyPreview());
    s.setDefaultStrategy(MockResponseStrategy.valueOf(e.getDefaultStrategy().name()));
    s.setIsEnabled(e.getIsEnabled());
    s.setCreatedAt(e.getCreatedAt());
    s.setUpdatedAt(e.getUpdatedAt());
    return s;
  }

  private MockServerConfig fromConfigEntity(MockServerConfigEntity e) {
    MockServerConfig c = new MockServerConfig();
    c.setConfigId(e.getConfigId());
    c.setMockServerId(e.getMockServerId());
    c.setMaxVariantsPerOperation(e.getMaxVariantsPerOperation());
    c.setMaxTotalVariants(e.getMaxTotalVariants());
    c.setMcpEnabled(e.isMcpEnabled());
    c.setSchemaValidationMode(
        e.getSchemaValidationMode() != null
            ? SchemaValidationMode.valueOf(e.getSchemaValidationMode().name())
            : SchemaValidationMode.OFF);
    return c;
  }

  private MockResponseVariant fromVariantEntity(MockResponseVariantEntity e) {
    MockResponseVariant v = new MockResponseVariant();
    v.setVariantId(e.getVariantId());
    v.setMockServerId(e.getMockServerId());
    v.setOperationId(e.getOperationId());
    v.setResponseName(e.getResponseName());
    v.setStatusCode(e.getStatusCode());
    v.setContentType(e.getContentType() != null ? e.getContentType() : ContentTypeConstants.ANY);
    v.setResponseBody(e.getResponseBody());
    v.setHeaders(e.getHeaders());
    v.setIsDefault(e.getIsDefault());
    v.setIsGenerated(e.getIsGenerated());
    v.setDisplayOrder(e.getDisplayOrder());
    v.setCelExpression(e.getCelExpression());
    v.setCreatedAt(e.getCreatedAt());
    return v;
  }

  private MockOperationConfig fromOpConfigEntity(MockOperationConfigEntity e) {
    MockOperationConfig c = new MockOperationConfig();
    c.setConfigId(e.getConfigId());
    c.setMockServerId(e.getMockServerId());
    c.setOperationId(e.getOperationId());
    c.setIsEnabled(e.getIsEnabled());
    if (e.getStrategyOverride() != null) {
      c.setStrategyOverride(MockResponseStrategy.valueOf(e.getStrategyOverride().name()));
    }
    c.setRoundRobinPosition(e.getRoundRobinPosition());
    return c;
  }

  private MockRequestLog fromLogEntity(MockRequestLogEntity e) {
    MockRequestLog log = new MockRequestLog();
    log.setLogId(e.getLogId());
    log.setMockServerId(e.getMockServerId());
    log.setOperationId(e.getOperationId());
    log.setRequestPath(e.getRequestPath());
    log.setRequestMethod(e.getRequestMethod());
    log.setResponseStatusCode(e.getResponseStatusCode());
    log.setVariantId(e.getVariantId());
    log.setRequestedAt(e.getRequestedAt());
    return log;
  }

  private MockServerOperation fromOperationEntity(MockServerOperationEntity e) {
    return new MockServerOperation(
        e.getSpecId(),
        e.getOperationId(),
        e.getHttpMethod(),
        e.getPath(),
        e.getSuccessStatusCode());
  }

  // ── Mapping: POJO → entity ───────────────────────────────────────────────

  private MockServerEntity toEntity(MockServer s) {
    MockServerEntity e = new MockServerEntity();
    e.setMockServerId(s.getMockServerId());
    e.setSpecId(s.getSpecId());
    e.setName(s.getName());
    e.setApiKeyHash(s.getApiKeyHash());
    e.setApiKeyPreview(s.getApiKeyPreview());
    e.setDefaultStrategy(
        io.spec0.mockserver.domain.MockResponseStrategy.valueOf(s.getDefaultStrategy().name()));
    e.setIsEnabled(s.getIsEnabled());
    return e;
  }

  private MockServerConfigEntity toConfigEntity(MockServerConfig c) {
    MockServerConfigEntity e =
        configRepository
            .findByMockServerId(c.getMockServerId())
            .orElse(new MockServerConfigEntity());
    e.setMockServerId(c.getMockServerId());
    e.setMaxVariantsPerOperation(c.getMaxVariantsPerOperation());
    e.setMaxTotalVariants(c.getMaxTotalVariants());
    e.setMcpEnabled(c.isMcpEnabled());
    if (c.getSchemaValidationMode() != null) {
      e.setSchemaValidationMode(
          io.spec0.mockserver.openapi.validation.SchemaValidationMode.valueOf(
              c.getSchemaValidationMode().name()));
    }
    return e;
  }

  private MockResponseVariantEntity toVariantEntity(MockResponseVariant v) {
    MockResponseVariantEntity e = new MockResponseVariantEntity();
    if (v.getVariantId() != null) e.setVariantId(v.getVariantId());
    e.setMockServerId(v.getMockServerId());
    e.setOperationId(v.getOperationId());
    e.setResponseName(v.getResponseName());
    e.setStatusCode(v.getStatusCode());
    e.setContentType(v.getContentType() != null ? v.getContentType() : ContentTypeConstants.ANY);
    e.setResponseBody(v.getResponseBody());
    e.setHeaders(v.getHeaders());
    e.setIsDefault(v.getIsDefault());
    e.setIsGenerated(v.getIsGenerated());
    e.setDisplayOrder(v.getDisplayOrder());
    e.setCelExpression(v.getCelExpression());
    return e;
  }

  private MockOperationConfigEntity toOpConfigEntity(MockOperationConfig c) {
    MockOperationConfigEntity e =
        c.getConfigId() != null
            ? operationConfigRepository
                .findById(c.getConfigId())
                .orElse(new MockOperationConfigEntity())
            : new MockOperationConfigEntity();
    e.setMockServerId(c.getMockServerId());
    e.setOperationId(c.getOperationId());
    e.setIsEnabled(c.getIsEnabled());
    if (c.getStrategyOverride() != null) {
      e.setStrategyOverride(
          io.spec0.mockserver.domain.MockResponseStrategy.valueOf(c.getStrategyOverride().name()));
    } else {
      e.setStrategyOverride(null);
    }
    e.setRoundRobinPosition(c.getRoundRobinPosition());
    return e;
  }
}
