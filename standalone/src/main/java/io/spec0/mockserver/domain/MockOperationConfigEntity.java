package io.spec0.mockserver.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-operation configuration within a mock server (strategy override, enabled flag, round-robin
 * state).
 */
@Entity
@Table(
    name = "mock_operation_configs",
    schema = "mock_server",
    indexes = {@Index(name = "idx_moc_server_op", columnList = "mock_server_id, operation_id")})
@Getter
@Setter
@NoArgsConstructor
public class MockOperationConfigEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "config_id")
  private UUID configId;

  @Column(name = "mock_server_id", nullable = false)
  private UUID mockServerId;

  @Column(name = "operation_id", nullable = false)
  private String operationId;

  @Column(name = "is_enabled", nullable = false)
  private Boolean isEnabled = true;

  /** When non-null, overrides the mock server's default strategy for this specific operation. */
  @Enumerated(EnumType.STRING)
  @Column(name = "strategy_override", length = 30)
  private MockResponseStrategy strategyOverride;

  /** Current position for ROUND_ROBIN strategy. Mutable on every request. */
  @Column(name = "round_robin_position", nullable = false)
  private int roundRobinPosition = 0;

  public MockOperationConfigEntity(UUID mockServerId, String operationId) {
    this.mockServerId = mockServerId;
    this.operationId = operationId;
  }
}
