package io.spec0.mockserver.port;

import io.spec0.mockserver.domain.MockResponseStrategy;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import io.spec0.mockserver.domain.MockServerEntity;
import io.spec0.mockserver.dto.MockServerExportDto;
import io.spec0.mockserver.dto.VariantCreateDto;
import io.spec0.mockserver.dto.VariantSaveResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Integration contract for mock server lifecycle and variant management. Platform backends (SaaS,
 * standalone) autowire this interface — never access repositories directly.
 */
public interface MockServerServicePort {

  /** Create a mock server for an already-registered spec. Auto-generates default variants. */
  MockServerEntity createMockServer(UUID specId, String name, MockResponseStrategy strategy);

  /**
   * Create a mock server and immediately populate it with the provided variants (used during
   * import).
   */
  MockServerEntity createMockServerWithVariants(
      UUID specId, String name, MockResponseStrategy strategy, List<VariantCreateDto> variants);

  Optional<MockServerEntity> findById(UUID mockServerId);

  List<MockServerEntity> findAll();

  List<MockServerEntity> findBySpecId(UUID specId);

  void deleteMockServer(UUID mockServerId);

  // --- Variant management ---

  List<MockResponseVariantEntity> getVariants(UUID mockServerId);

  List<MockResponseVariantEntity> getVariantsByOperationId(UUID mockServerId, String operationId);

  MockResponseVariantEntity createVariant(UUID mockServerId, VariantCreateDto dto);

  MockResponseVariantEntity updateVariant(UUID mockServerId, UUID variantId, VariantCreateDto dto);

  VariantSaveResult createVariantFull(UUID mockServerId, VariantCreateDto dto);

  VariantSaveResult updateVariantFull(UUID mockServerId, UUID variantId, VariantCreateDto dto);

  void deleteVariant(UUID mockServerId, UUID variantId);

  // --- Management ---

  MockServerEntity updateName(UUID mockServerId, String newName);

  void setEnabled(UUID mockServerId, boolean enabled);

  MockServerEntity updateStrategy(UUID mockServerId, MockResponseStrategy strategy);

  // --- Logs ---

  List<io.spec0.mockserver.domain.MockRequestLogEntity> getRecentLogs(UUID mockServerId, int limit);

  void clearLogs(UUID mockServerId);

  // --- Operation configs ---

  List<io.spec0.mockserver.domain.MockOperationConfigEntity> getOperationConfigs(UUID mockServerId);

  io.spec0.mockserver.domain.MockOperationConfigEntity updateOperationConfig(
      UUID mockServerId,
      String operationId,
      Boolean enabled,
      MockResponseStrategy strategyOverride);

  // --- Server config ---

  Optional<io.spec0.mockserver.domain.MockServerConfigEntity> getMockServerConfig(
      UUID mockServerId);

  // --- Export ---

  MockServerExportDto exportMockServer(UUID mockServerId);
}
