package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Per-mock-server environment variable accessible in CEL expressions as {@code env.MY_KEY}.
 * Stored in the {@code mock_server_env_vars} table.
 */
@Entity
@Table(
    name = "mock_server_env_vars",
    schema = "mock_server",
    indexes = {
      @Index(name = "idx_mev_server_id", columnList = "mock_server_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class MockServerEnvVarEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "env_var_id")
  private UUID envVarId;

  @Column(name = "mock_server_id", nullable = false)
  private UUID mockServerId;

  @Column(name = "var_key", nullable = false, length = 255)
  private String varKey;

  @Column(name = "var_value", nullable = false, columnDefinition = "text")
  private String varValue;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  public MockServerEnvVarEntity(UUID mockServerId, String varKey, String varValue) {
    this.mockServerId = mockServerId;
    this.varKey = varKey;
    this.varValue = varValue;
  }
}
