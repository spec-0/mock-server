package io.spec0.mockserver.port;

import io.spec0.mockserver.domain.ApiSpecEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Integration contract for spec management. Platform backends (SaaS, standalone) autowire this
 * interface to interact with the mock server core — never via repositories directly.
 */
public interface ApiSpecServicePort {

  /**
   * Register a new spec. If a spec with the same name and hash already exists, returns the existing
   * one (idempotent).
   */
  ApiSpecEntity registerSpec(String specName, String specContent);

  Optional<ApiSpecEntity> findById(UUID specId);

  Optional<ApiSpecEntity> findByNameAndHash(String specName, String specHash);

  boolean existsByNameAndHash(String specName, String specHash);
}
