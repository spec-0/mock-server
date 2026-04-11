package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockOperationConfigEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockOperationConfigRepository
    extends JpaRepository<MockOperationConfigEntity, UUID> {

  List<MockOperationConfigEntity> findByMockServerId(UUID mockServerId);

  Optional<MockOperationConfigEntity> findByMockServerIdAndOperationId(
      UUID mockServerId, String operationId);

  void deleteByMockServerId(UUID mockServerId);
}
