package io.spec0.mockserver.engine.model;

import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockServerConfig {

  private UUID configId;
  private UUID mockServerId;
  private int maxVariantsPerOperation = 10;
  private int maxTotalVariants = 100;
  private boolean mcpEnabled = false;
  private SchemaValidationMode schemaValidationMode = SchemaValidationMode.OFF;

  public MockServerConfig(UUID mockServerId) {
    this.mockServerId = mockServerId;
  }
}
