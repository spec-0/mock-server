package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request log entry for a mock server. Lightweight — no analytics aggregation. */
@Entity
@Table(
    name = "mock_request_logs",
    schema = "mock_server",
    indexes = {
      @Index(name = "idx_mrl_server_id", columnList = "mock_server_id"),
      @Index(name = "idx_mrl_requested_at", columnList = "requested_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class MockRequestLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "log_id")
  private UUID logId;

  @Column(name = "mock_server_id", nullable = false)
  private UUID mockServerId;

  @Column(name = "operation_id")
  private String operationId;

  @Column(name = "request_path", nullable = false)
  private String requestPath;

  @Column(name = "request_method", nullable = false, length = 10)
  private String requestMethod;

  @Column(name = "response_status_code", length = 10)
  private String responseStatusCode;

  @Column(name = "variant_id")
  private UUID variantId;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  public MockRequestLogEntity(
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
  }
}
