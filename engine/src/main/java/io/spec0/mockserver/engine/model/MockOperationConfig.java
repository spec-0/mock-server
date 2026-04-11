package io.spec0.mockserver.engine.model;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockOperationConfig {

  private UUID configId;
  private UUID mockServerId;
  private String operationId;
  private Boolean isEnabled = true;
  private MockResponseStrategy strategyOverride;
  private int roundRobinPosition = 0;

  public MockOperationConfig(UUID mockServerId, String operationId) {
    this.mockServerId = mockServerId;
    this.operationId = operationId;
  }
}
