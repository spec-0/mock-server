package io.spec0.mockserver.engine.dto;

import io.spec0.mockserver.engine.model.MockResponseStrategy;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockServerExportDto {

  private String version = "1.0";
  private SpecSnapshot spec;
  private ServerSnapshot mockServer;
  private List<VariantSnapshot> variants;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SpecSnapshot {
    private String name;
    private String content;
    private String hash;
    private String specVersion;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServerSnapshot {
    private String name;
    private MockResponseStrategy defaultStrategy;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VariantSnapshot {
    private String operationId;
    private String responseName;
    private String statusCode;
    private String responseBody;
    private String headers;

    @com.fasterxml.jackson.annotation.JsonProperty("isDefault")
    private boolean defaultVariant;

    private int displayOrder;
  }
}
