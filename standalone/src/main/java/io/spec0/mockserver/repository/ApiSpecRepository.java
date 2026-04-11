package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.ApiSpecEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiSpecRepository extends JpaRepository<ApiSpecEntity, UUID> {

  Optional<ApiSpecEntity> findBySpecNameAndSpecHash(String specName, String specHash);

  boolean existsBySpecNameAndSpecHash(String specName, String specHash);
}
