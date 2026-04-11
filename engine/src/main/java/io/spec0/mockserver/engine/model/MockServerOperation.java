package io.spec0.mockserver.engine.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockServerOperation {

  private UUID id;
  private UUID specId;
  private String operationId;
  private String httpMethod;
  private String path;
  private String successStatusCode;
  private LocalDateTime createdAt;

  public MockServerOperation(
      UUID specId,
      String operationId,
      String httpMethod,
      String path,
      String successStatusCode) {
    this.specId = specId;
    this.operationId = operationId;
    this.httpMethod = httpMethod;
    this.path = path;
    this.successStatusCode = successStatusCode;
  }
}
