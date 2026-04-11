package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockServerEnvVarEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockServerEnvVarRepository extends JpaRepository<MockServerEnvVarEntity, UUID> {

  List<MockServerEnvVarEntity> findByMockServerId(UUID mockServerId);

  Optional<MockServerEnvVarEntity> findByMockServerIdAndVarKey(UUID mockServerId, String varKey);

  void deleteByMockServerId(UUID mockServerId);
}
