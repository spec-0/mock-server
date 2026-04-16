package io.spec0.mockserver.engine.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
  private List<MockRequestLogMetric> metrics = new ArrayList<>();

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
