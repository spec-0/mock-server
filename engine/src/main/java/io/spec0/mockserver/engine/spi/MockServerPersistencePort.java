package io.spec0.mockserver.engine.spi;

import io.spec0.mockserver.engine.model.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence port — framework-agnostic storage for mock-server state. Spring and Quarkus
 * adapters implement this with JPA/JDBC/etc.
 */
public interface MockServerPersistencePort {

  MockServer saveMockServer(MockServer server);

  Optional<MockServer> findMockServerById(UUID mockServerId);

  List<MockServer> findAllMockServers();

  List<MockServer> findMockServersBySpecId(UUID specId);

  void deleteMockServerById(UUID mockServerId);

  MockServerConfig saveMockServerConfig(MockServerConfig config);

  Optional<MockServerConfig> findConfigByMockServerId(UUID mockServerId);

  void deleteConfigById(UUID configId);

  MockResponseVariant saveVariant(MockResponseVariant variant);

  Optional<MockResponseVariant> findVariantById(UUID variantId);

  List<MockResponseVariant> findVariantsByMockServerIdOrderByDisplayOrder(UUID mockServerId);

  List<MockResponseVariant> findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
      UUID mockServerId, String operationId);

  Optional<MockResponseVariant> findFirstDefaultVariant(UUID mockServerId, String operationId);

  long countVariantsByMockServerId(UUID mockServerId);

  long countVariantsByMockServerIdAndOperationId(UUID mockServerId, String operationId);

  void deleteVariantById(UUID variantId);

  void deleteVariantsByMockServerId(UUID mockServerId);

  MockOperationConfig saveOperationConfig(MockOperationConfig config);

  List<MockOperationConfig> findOperationConfigsByMockServerId(UUID mockServerId);

  Optional<MockOperationConfig> findOperationConfigByMockServerIdAndOperationId(
      UUID mockServerId, String operationId);

  void deleteOperationConfigsByMockServerId(UUID mockServerId);

  void saveRequestLog(MockRequestLog log);

  List<MockRequestLog> findRecentLogsByMockServerId(UUID mockServerId, int limit);

  void deleteLogsByMockServerId(UUID mockServerId);

  void deleteEnvVarsByMockServerId(UUID mockServerId);

  List<MockServerOperation> findOperationsBySpecId(UUID specId);
}
