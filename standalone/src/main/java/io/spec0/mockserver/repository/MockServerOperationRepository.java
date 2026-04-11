package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockServerOperationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockServerOperationRepository
    extends JpaRepository<MockServerOperationEntity, UUID> {

  List<MockServerOperationEntity> findBySpecId(UUID specId);

  void deleteBySpecId(UUID specId);
}
