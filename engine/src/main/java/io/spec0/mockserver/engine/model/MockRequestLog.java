package io.spec0.mockserver.engine.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockRequestLog {

  private UUID logId;
  private UUID mockServerId;
  private String operationId;
  private String requestPath;
  private String requestMethod;
  private String responseStatusCode;
  private UUID variantId;
  private LocalDateTime requestedAt;

  /** Optional per-request metrics (latency, etc.); persisted by adapters that support them. */
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private List<MockRequestLogMetric> metrics = new ArrayList<>();

  public List<MockRequestLogMetric> getMetrics() {
    return Collections.unmodifiableList(metrics);
  }

  public void setMetrics(List<MockRequestLogMetric> metrics) {
    this.metrics = metrics == null ? new ArrayList<>() : new ArrayList<>(metrics);
  }

  /** Appends a metric; {@link #getMetrics()} remains an unmodifiable view. */
  public void addMetric(MockRequestLogMetric metric) {
    if (metric != null) {
      metrics.add(metric);
    }
  }

  public MockRequestLog(
      UUID mockServerId,
      String operationId,
      String requestPath,
      String requestMethod,
      String responseStatusCode,
      UUID variantId) {
    this.mockServerId = mockServerId;
    this.operationId = operationId;
    this.requestPath = requestPath;
    this.requestMethod = requestMethod;
    this.responseStatusCode = responseStatusCode;
    this.variantId = variantId;
    this.requestedAt = LocalDateTime.now();
    this.metrics = new ArrayList<>();
  }
}
