package io.spec0.mockserver.engine.domain;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockServerOperation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-memory view of a registered API spec and its extracted operations. Persistence remains flat in
 * adapters; this aggregate is built after load or registration for orchestration and APIs.
 */
public final class ApiSpecAggregate {

  private final ApiSpecSnapshot spec;
  private final List<MockServerOperation> operations;

  public ApiSpecAggregate(ApiSpecSnapshot spec, List<MockServerOperation> operations) {
    this.spec = Objects.requireNonNull(spec);
    this.operations =
        operations == null ? List.of() : Collections.unmodifiableList(List.copyOf(operations));
  }

  public static ApiSpecAggregate fromRegistration(
      ApiSpecSnapshot spec, List<MockServerOperation> operations) {
    return new ApiSpecAggregate(spec, operations);
  }

  public ApiSpecSnapshot spec() {
    return spec;
  }

  public List<MockServerOperation> operations() {
    return operations;
  }
}
