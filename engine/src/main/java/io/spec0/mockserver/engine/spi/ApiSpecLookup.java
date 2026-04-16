package io.spec0.mockserver.engine.spi;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port: read registered API specs by id (implemented by each runtime adapter). For
 * registering new specs and persisting operations, use {@link ApiSpecCatalogPersistencePort} with
 * {@link io.spec0.mockserver.engine.service.DefaultApiSpecRegistrationService}.
 */
public interface ApiSpecLookup {

  Optional<ApiSpecSnapshot> findSpecById(UUID specId);
}
