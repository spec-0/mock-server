package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Parsed operation from an ApiSpec. Populated when the spec is registered. Used for operation
 * resolution during mock request handling (path matching + method matching).
 */
@Entity
@Table(
    name = "mock_server_operations",
    schema = "mock_server",
    indexes = {@Index(name = "idx_ms_ops_spec_id", columnList = "spec_id")})
@Getter
@Setter
@NoArgsConstructor
public class MockServerOperationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @Column(name = "spec_id", nullable = false)
  private UUID specId;

  /** The operationId from the spec (or synthesised as "{method}_{path_slug}"). */
  @Column(name = "operation_id", nullable = false)
  private String operationId;

  /** Uppercase HTTP method: GET, POST, PUT, DELETE, PATCH. */
  @Column(name = "http_method", nullable = false, length = 10)
  private String httpMethod;

  /** Raw path as in the spec, e.g. /users/{id}. */
  @Column(name = "path", nullable = false)
  private String path;

  /** First 2xx status code found in the spec responses for this operation. */
  @Column(name = "success_status_code", nullable = false, length = 10)
  private String successStatusCode;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  public MockServerOperationEntity(
      UUID specId, String operationId, String httpMethod, String path, String successStatusCode) {
    this.specId = specId;
    this.operationId = operationId;
    this.httpMethod = httpMethod;
    this.path = path;
    this.successStatusCode = successStatusCode;
  }
}
