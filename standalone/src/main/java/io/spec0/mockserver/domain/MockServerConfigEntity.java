package io.spec0.mockserver.domain;

import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Global configuration for a mock server instance (limits, default strategy). */
@Entity
@Table(name = "mock_server_configs", schema = "mock_server")
@Getter
@Setter
@NoArgsConstructor
public class MockServerConfigEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "config_id")
  private UUID configId;

  @Column(name = "mock_server_id", nullable = false, unique = true)
  private UUID mockServerId;

  @Column(name = "max_variants_per_operation", nullable = false)
  private int maxVariantsPerOperation = 10;

  @Column(name = "max_total_variants", nullable = false)
  private int maxTotalVariants = 100;

  /** When true, the MCP server endpoints (/mcp/sse, /mcp/message) are active. */
  @Column(name = "mcp_enabled", nullable = false)
  private boolean mcpEnabled = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "schema_validation_mode", nullable = false, length = 16)
  private SchemaValidationMode schemaValidationMode = SchemaValidationMode.OFF;

  public MockServerConfigEntity(UUID mockServerId) {
    this.mockServerId = mockServerId;
  }
}
