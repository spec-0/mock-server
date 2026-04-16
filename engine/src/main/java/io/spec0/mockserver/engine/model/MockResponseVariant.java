package io.spec0.mockserver.engine.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockResponseVariant {

  private UUID variantId;
  private UUID mockServerId;
  private String operationId;
  private String responseName;
  private String statusCode;

  /** Normalized media type for this variant; {@link ContentTypeConstants#ANY} when unspecified. */
  private String contentType = ContentTypeConstants.ANY;

  private String responseBody;
  private String headers;
  private Boolean isDefault = false;
  private Boolean isGenerated = false;
  private Integer displayOrder = 0;
  private String celExpression;
  private LocalDateTime createdAt;

  public MockResponseVariant(
      UUID mockServerId,
      String operationId,
      String responseName,
      String statusCode,
      String responseBody) {
    this.mockServerId = mockServerId;
    this.operationId = operationId;
    this.responseName = responseName;
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }
}
