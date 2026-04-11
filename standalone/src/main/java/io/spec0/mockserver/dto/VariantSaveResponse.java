package io.spec0.mockserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import java.util.List;

public class VariantSaveResponse {

  @JsonUnwrapped private final MockResponseVariantEntity variant;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final List<String> validationWarnings;

  public VariantSaveResponse(VariantSaveResult result) {
    this.variant = result.entity();
    List<String> w = result.validationWarnings();
    this.validationWarnings = (w != null && !w.isEmpty()) ? w : null;
  }

  public MockResponseVariantEntity getVariant() {
    return variant;
  }

  public List<String> getValidationWarnings() {
    return validationWarnings;
  }
}
