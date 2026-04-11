package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents an immutable OpenAPI specification. Once registered, specContent cannot be changed — a
 * spec update always creates a new ApiSpecEntity with a new specId.
 */
@Entity
@Table(name = "api_specs", schema = "mock_server")
@Getter
@Setter
@NoArgsConstructor
public class ApiSpecEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "spec_id")
  private UUID specId;

  /** Human-readable kebab-case name (e.g. "payments-api"). */
  @Column(name = "spec_name", nullable = false)
  private String specName;

  /** Raw YAML or JSON content of the OpenAPI spec. Write-once. */
  @Column(name = "spec_content", nullable = false, columnDefinition = "text")
  private String specContent;

  /**
   * SHA-256 hex digest of specContent — used to detect spec changes and enable idempotent imports.
   */
  @Column(name = "spec_hash", nullable = false, length = 64)
  private String specHash;

  /** Version string parsed from info.version in the spec. */
  @Column(name = "spec_version", length = 100)
  private String specVersion;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  public ApiSpecEntity(String specName, String specContent, String specHash, String specVersion) {
    this.specName = specName;
    this.specContent = specContent;
    this.specHash = specHash;
    this.specVersion = specVersion;
  }
}
