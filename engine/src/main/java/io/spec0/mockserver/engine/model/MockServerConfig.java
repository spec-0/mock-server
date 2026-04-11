package io.spec0.mockserver.engine.model;

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

  public MockServerConfig(UUID mockServerId) {
    this.mockServerId = mockServerId;
  }
}
