package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockRequestLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockRequestLogRepository extends JpaRepository<MockRequestLogEntity, UUID> {

  List<MockRequestLogEntity> findByMockServerIdOrderByRequestedAtDesc(
      UUID mockServerId, Pageable pageable);

  void deleteByMockServerId(UUID mockServerId);
}
