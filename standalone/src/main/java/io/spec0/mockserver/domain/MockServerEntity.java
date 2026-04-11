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
 * A mock server instance, always linked to an ApiSpec. The specId is immutable after creation.
 * Contains no org/team references — those are managed by the consuming platform via a loose UUID
 * reference.
 */
@Entity
@Table(name = "mock_servers", schema = "mock_server")
@Getter
@Setter
@NoArgsConstructor
public class MockServerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "mock_server_id")
  private UUID mockServerId;

  /** FK to ApiSpecEntity. Immutable after creation. */
  @Column(name = "spec_id", nullable = false, updatable = false)
  private UUID specId;

  @Column(name = "name", nullable = false)
  private String name;

  /**
   * Optional API key for securing mock requests. Stored as plain text in standalone mode. May be
   * hashed by the consuming platform.
   */
  @Column(name = "api_key_hash")
  private String apiKeyHash;

  @Column(name = "api_key_preview", length = 20)
  private String apiKeyPreview;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_strategy", nullable = false, length = 30)
  private MockResponseStrategy defaultStrategy = MockResponseStrategy.RANDOM;

  @Column(name = "is_enabled", nullable = false)
  private Boolean isEnabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  public MockServerEntity(UUID specId, String name, MockResponseStrategy defaultStrategy) {
    this.specId = specId;
    this.name = name;
    this.defaultStrategy = defaultStrategy;
  }
}
