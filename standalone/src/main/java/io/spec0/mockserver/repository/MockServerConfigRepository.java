package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockServerConfigEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockServerConfigRepository extends JpaRepository<MockServerConfigEntity, UUID> {

  Optional<MockServerConfigEntity> findByMockServerId(UUID mockServerId);
}
