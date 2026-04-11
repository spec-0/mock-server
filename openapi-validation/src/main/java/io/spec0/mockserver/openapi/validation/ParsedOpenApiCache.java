package io.spec0.mockserver.openapi.validation;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.UUID;

/** Parsed {@link OpenAPI} per spec id, with explicit invalidation when specs change. */
public interface ParsedOpenApiCache {

  OpenAPI get(UUID specId);

  void invalidate(UUID specId);

  void invalidateAll();
}
