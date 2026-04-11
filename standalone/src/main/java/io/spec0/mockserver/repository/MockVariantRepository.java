package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockResponseVariantEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockVariantRepository extends JpaRepository<MockResponseVariantEntity, UUID> {

  List<MockResponseVariantEntity> findByMockServerIdOrderByDisplayOrder(UUID mockServerId);

  List<MockResponseVariantEntity> findByMockServerIdAndOperationIdOrderByDisplayOrder(
      UUID mockServerId, String operationId);

  Optional<MockResponseVariantEntity> findFirstByMockServerIdAndOperationIdAndIsDefaultTrue(
      UUID mockServerId, String operationId);

  long countByMockServerId(UUID mockServerId);

  long countByMockServerIdAndOperationId(UUID mockServerId, String operationId);

  void deleteByMockServerId(UUID mockServerId);
}
