package io.spec0.mockserver.engine.service;

import io.spec0.mockserver.engine.model.MockOperationConfig;
import io.spec0.mockserver.engine.model.MockRequestLog;
import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import io.spec0.mockserver.engine.model.MockServerConfig;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Minimal in-memory persistence covering only what the create-mock path needs. */
final class InMemoryPersistence implements MockServerPersistencePort {
  private final AtomicLong ids = new AtomicLong();
  private final List<MockResponseVariant> variants = new ArrayList<>();
  final java.util.Map<UUID, List<MockServerOperation>> operationsBySpec = new java.util.HashMap<>();

  @Override
  public MockServer saveMockServer(MockServer server) {
    if (server.getMockServerId() == null) {
      server.setMockServerId(UUID.randomUUID());
    }
    return server;
  }

  @Override
  public MockServerConfig saveMockServerConfig(MockServerConfig config) {
    if (config.getConfigId() == null) {
      config.setConfigId(UUID.randomUUID());
    }
    return config;
  }

  @Override
  public MockResponseVariant saveVariant(MockResponseVariant variant) {
    if (variant.getVariantId() == null) {
      variant.setVariantId(UUID.randomUUID());
    }
    variants.add(variant);
    return variant;
  }

  @Override
  public List<MockResponseVariant> findVariantsByMockServerIdOrderByDisplayOrder(
      UUID mockServerId) {
    return variants.stream().filter(v -> v.getMockServerId().equals(mockServerId)).toList();
  }

  @Override
  public List<MockServerOperation> findOperationsBySpecId(UUID specId) {
    return operationsBySpec.getOrDefault(specId, List.of());
  }

  @Override
  public MockOperationConfig saveOperationConfig(MockOperationConfig config) {
    return config;
  }

  // ── Unused by the create-mock path ────────────────────────────────────────
  @Override
  public Optional<MockServer> findMockServerById(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MockServer> findAllMockServers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MockServer> findMockServersBySpecId(UUID specId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMockServerById(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<MockServerConfig> findConfigByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteConfigById(UUID configId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<MockResponseVariant> findVariantById(UUID variantId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MockResponseVariant> findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
      UUID mockServerId, String operationId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<MockResponseVariant> findFirstDefaultVariant(
      UUID mockServerId, String operationId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long countVariantsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long countVariantsByMockServerIdAndOperationId(UUID mockServerId, String operationId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteVariantById(UUID variantId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteVariantsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MockOperationConfig> findOperationConfigsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<MockOperationConfig> findOperationConfigByMockServerIdAndOperationId(
      UUID mockServerId, String operationId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteOperationConfigsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveRequestLog(MockRequestLog log) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MockRequestLog> findRecentLogsByMockServerId(UUID mockServerId, int limit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteLogsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteEnvVarsByMockServerId(UUID mockServerId) {
    throw new UnsupportedOperationException();
  }
}
