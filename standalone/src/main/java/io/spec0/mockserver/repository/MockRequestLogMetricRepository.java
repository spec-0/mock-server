package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockRequestLogMetricEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockRequestLogMetricRepository
    extends JpaRepository<MockRequestLogMetricEntity, UUID> {}
