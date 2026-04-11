package io.spec0.mockserver.engine.spi;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: read registered API specs by id (implemented by each runtime adapter). */
public interface ApiSpecLookup {

  Optional<ApiSpecSnapshot> findSpecById(UUID specId);
}
