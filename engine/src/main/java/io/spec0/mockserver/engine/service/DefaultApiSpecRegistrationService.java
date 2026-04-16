package io.spec0.mockserver.engine.service;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.openapi.OpenApiSpecSupport;
import io.spec0.mockserver.engine.spi.ApiSpecCatalogPersistencePort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers an OpenAPI document in the catalog: idempotent by (name, content hash), extracts
 * operations in-engine, persists via {@link ApiSpecCatalogPersistencePort}. Transaction boundaries
 * are enforced by the adapter or the calling Spring {@code @Transactional} facade.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultApiSpecRegistrationService {

  private final ApiSpecCatalogPersistencePort catalog;

  public ApiSpecSnapshot registerSpec(String specName, String specContent) {
    String hash = OpenApiSpecSupport.sha256(specContent);
    Optional<ApiSpecSnapshot> existing = catalog.findByNameAndHash(specName, hash);
    if (existing.isPresent()) {
      log.info("Spec '{}' with same hash already registered, returning existing", specName);
      return existing.get();
    }
    String version = OpenApiSpecSupport.parseInfoVersion(specContent);
    List<MockServerOperation> operations = OpenApiSpecSupport.extractOperations(null, specContent);
    ApiSpecSnapshot saved =
        catalog.saveNewSpecWithOperations(specName, specContent, hash, version, operations);
    log.info(
        "Registered spec '{}' (id={}) with {} operations",
        specName,
        saved.specId(),
        operations.size());
    return saved;
  }

  public Optional<ApiSpecSnapshot> findById(java.util.UUID specId) {
    return catalog.findById(specId);
  }
}
