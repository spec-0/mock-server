package io.spec0.mockserver.openapi.validation;

import java.util.Optional;
import java.util.UUID;

/** Loads raw OpenAPI YAML/JSON for a registered spec id (typically from a database). */
public interface SpecContentLoader {

  Optional<String> loadContent(UUID specId);
}
