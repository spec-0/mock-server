package io.spec0.mockserver.engine.api;

import io.spec0.mockserver.engine.dto.MockServerExportDto;
import io.spec0.mockserver.engine.dto.VariantCreateDto;
import io.spec0.mockserver.engine.model.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application API implemented by {@link io.spec0.mockserver.engine.service.DefaultMockServerService}. */
public interface MockServerServicePort {

  MockServer createMockServer(UUID specId, String name, MockResponseStrategy strategy);

  MockServer createMockServerWithVariants(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> variants);

  Optional<MockServer> findById(UUID mockServerId);

  List<MockServer> findAll();

  List<MockServer> findBySpecId(UUID specId);

  void deleteMockServer(UUID mockServerId);

  List<MockResponseVariant> getVariants(UUID mockServerId);

  List<MockResponseVariant> getVariantsByOperationId(UUID mockServerId, String operationId);

  MockResponseVariant createVariant(UUID mockServerId, VariantCreateDto dto);

  MockResponseVariant updateVariant(UUID mockServerId, UUID variantId, VariantCreateDto dto);

  void deleteVariant(UUID mockServerId, UUID variantId);

  MockServer updateName(UUID mockServerId, String newName);

  void setEnabled(UUID mockServerId, boolean enabled);

  MockServer updateStrategy(UUID mockServerId, MockResponseStrategy strategy);

  List<MockRequestLog> getRecentLogs(UUID mockServerId, int limit);

  void clearLogs(UUID mockServerId);

  List<MockOperationConfig> getOperationConfigs(UUID mockServerId);

  MockOperationConfig updateOperationConfig(
      UUID mockServerId, String operationId, Boolean enabled, MockResponseStrategy strategyOverride);

  Optional<MockServerConfig> getMockServerConfig(UUID mockServerId);

  MockServerExportDto exportMockServer(UUID mockServerId);

  /** Invoked by the HTTP mock handler after serving a response. */
  void logRequest(
      UUID mockServerId,
      String operationId,
      String requestPath,
      String requestMethod,
      String responseStatus,
      UUID variantId);
}
