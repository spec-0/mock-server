package io.spec0.mockserver.engine.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockServer {

  private UUID mockServerId;
  private UUID specId;
  private String name;
  private String apiKeyHash;
  private String apiKeyPreview;
  private MockResponseStrategy defaultStrategy = MockResponseStrategy.RANDOM;
  private Boolean isEnabled = true;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public MockServer(UUID specId, String name, MockResponseStrategy defaultStrategy) {
    this.specId = specId;
    this.name = name;
    this.defaultStrategy = defaultStrategy;
  }
}
