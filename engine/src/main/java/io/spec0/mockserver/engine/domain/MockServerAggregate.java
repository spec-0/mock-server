package io.spec0.mockserver.engine.domain;

import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** In-memory aggregate: mock server metadata plus all variants (flat list from persistence). */
public final class MockServerAggregate {

  private final MockServer server;
  private final List<MockResponseVariant> variants;

  public MockServerAggregate(MockServer server, List<MockResponseVariant> variants) {
    this.server = Objects.requireNonNull(server);
    this.variants =
        variants == null ? List.of() : Collections.unmodifiableList(List.copyOf(variants));
  }

  public MockServer server() {
    return server;
  }

  public List<MockResponseVariant> variants() {
    return variants;
  }
}
