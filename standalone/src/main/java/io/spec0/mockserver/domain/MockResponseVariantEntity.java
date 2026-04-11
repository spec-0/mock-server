package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A specific mock response variant for an operation. Multiple variants can exist per (mockServerId,
 * operationId, statusCode) combination. The active variant returned depends on the configured
 * MockResponseStrategy.
 */
@Entity
@Table(
    name = "mock_response_variants",
    schema = "mock_server",
    indexes = {
      @Index(name = "idx_mrv_server_op", columnList = "mock_server_id, operation_id"),
      @Index(name = "idx_mrv_server_default", columnList = "mock_server_id, is_default")
    })
@Getter
@Setter
@NoArgsConstructor
public class MockResponseVariantEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "variant_id")
  private UUID variantId;

  @Column(name = "mock_server_id", nullable = false)
  private UUID mockServerId;

  @Column(name = "operation_id", nullable = false)
  private String operationId;

  @Column(name = "response_name", nullable = false)
  private String responseName;

  @Column(name = "status_code", nullable = false, length = 10)
  private String statusCode;

  /** JSON string of the response body. Stored as TEXT — compatible with both H2 and Postgres. */
  @Column(name = "response_body", columnDefinition = "text")
  private String responseBody;

  /** JSON string of response headers (key-value map). Stored as TEXT. */
  @Column(name = "headers", columnDefinition = "text")
  private String headers;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  /** True if auto-generated from the spec schema; false if user-created. */
  @Column(name = "is_generated", nullable = false)
  private Boolean isGenerated = false;

  /** Lower value = higher priority in SEQUENTIAL strategy. */
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  /**
   * Optional CEL expression. When non-null, this is a CEL variant: the expression is evaluated at
   * request time and its result overrides the response. If the expression throws or returns null,
   * falls back to the static {@code responseBody}. When null, this is a static variant.
   */
  @Column(name = "cel_expression", columnDefinition = "text")
  private String celExpression;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  public MockResponseVariantEntity(
      UUID mockServerId,
      String operationId,
      String responseName,
      String statusCode,
      String responseBody) {
    this.mockServerId = mockServerId;
    this.operationId = operationId;
    this.responseName = responseName;
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }
}
