package io.spec0.mockserver.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantCreateDto {
  private String operationId;
  private String responseName;
  private String statusCode;
  private String responseBody;
  private String headers;
  private boolean isDefault;
  private Integer displayOrder;
  private String celExpression;
}
