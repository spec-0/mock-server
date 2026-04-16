package io.spec0.mockserver.engine.spi;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockServerOperation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting registered API specs and their extracted operations. Implemented by
 * standalone/platform adapters (JPA).
 */
public interface ApiSpecCatalogPersistencePort {

  Optional<ApiSpecSnapshot> findByNameAndHash(String specName, String specHash);

  Optional<ApiSpecSnapshot> findById(UUID specId);

  /**
   * Atomically persists a new spec row and its operations (adapter should run in one transaction).
   * {@link MockServerOperation#getSpecId()} on the list may be null before save; the adapter
   * assigns the persisted spec id to each row.
   */
  ApiSpecSnapshot saveNewSpecWithOperations(
      String specName,
      String specContent,
      String specHash,
      String specVersion,
      List<MockServerOperation> operations);
}
