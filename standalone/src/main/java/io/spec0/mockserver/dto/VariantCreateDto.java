package io.spec0.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for creating or updating a mock response variant. */
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

  /**
   * Optional CEL expression. When set, this is a CEL variant evaluated at request time. The
   * expression receives {@code request} and {@code env} context variables and must return a map
   * with {@code status} (int), {@code body} (any), and optional {@code headers} (map). When null,
   * this is a static variant and {@code responseBody} is used directly.
   */
  private String celExpression;
}
