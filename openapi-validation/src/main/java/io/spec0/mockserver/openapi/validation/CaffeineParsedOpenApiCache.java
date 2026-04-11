package io.spec0.mockserver.openapi.validation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.v3.oas.models.OpenAPI;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thread-safe cache of parsed OpenAPI models; loads via {@link SpecContentLoader} on miss. */
public final class CaffeineParsedOpenApiCache implements ParsedOpenApiCache {

  private static final Logger log = LoggerFactory.getLogger(CaffeineParsedOpenApiCache.class);

  private final Cache<UUID, OpenAPI> cache;
  private final SpecContentLoader loader;
  private final OpenApiSpecParser parser;

  public CaffeineParsedOpenApiCache(
      SpecContentLoader loader, OpenApiSpecParser parser, Duration ttl, long maximumSize) {
    this.loader = Objects.requireNonNull(loader);
    this.parser = Objects.requireNonNull(parser);
    this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maximumSize).build();
  }

  @Override
  public OpenAPI get(UUID specId) {
    return cache.get(
        specId,
        id -> {
          if (log.isTraceEnabled()) {
            log.trace("parsedOpenApiCache miss specId={} (loading and parsing)", id);
          }
          return parser.parse(
              loader
                  .loadContent(id)
                  .orElseThrow(() -> new IllegalArgumentException("Unknown spec id: " + id)));
        });
  }

  @Override
  public void invalidate(UUID specId) {
    cache.invalidate(specId);
  }

  @Override
  public void invalidateAll() {
    cache.invalidateAll();
  }
}
