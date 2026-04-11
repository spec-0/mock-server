package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockServerEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockServerRepository extends JpaRepository<MockServerEntity, UUID> {

  List<MockServerEntity> findBySpecId(UUID specId);

  List<MockServerEntity> findByIsEnabledTrue();
}
