package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.spec0.mockserver.openapi.validation.OpenApiSpecJson;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;

public class OpenApiParser {
  /** Path-item fields that are HTTP operations (OpenAPI 3.x); others are skipped. */
  private static final Set<String> HTTP_METHODS =
      Set.of("get", "post", "put", "delete", "patch", "options", "head", "trace");

  private final ObjectMapper jsonMapper = new ObjectMapper();

  // Raise SnakeYAML's per-document code-point limit so large specs (>3 MB) parse. SnakeYAML 2.x
  // defaults to 3 MB; see OpenApiSpecJson#MAX_SPEC_CODE_POINTS.
  private final YAMLMapper yamlMapper = buildYamlMapper();

  private static YAMLMapper buildYamlMapper() {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setCodePointLimit(OpenApiSpecJson.MAX_SPEC_CODE_POINTS);
    return new YAMLMapper(YAMLFactory.builder().loaderOptions(loaderOptions).build());
  }

  public JsonNode parseSpec(File file) throws IOException {
    if (file.getName().endsWith(".yaml")
        || file.getName().endsWith(".yml")
        || file.getName().endsWith(".json")) {

      if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
        return yamlMapper.readTree(file);
      } else {
        return jsonMapper.readTree(file);
      }
    } else {

      throw new IllegalArgumentException("invalid format of the file");
    }
  }

  public Map<String, Map<String, JsonNode>> extractOperations(JsonNode rootNode) {
    Map<String, Map<String, JsonNode>> apiOperations = new HashMap<>();

    JsonNode paths = rootNode.get("paths");
    if (paths == null) return apiOperations;

    paths
        .fields()
        .forEachRemaining(
            pathEntry -> {
              String path = pathEntry.getKey();
              JsonNode pathItem = pathEntry.getValue();
              pathItem
                  .fields()
                  .forEachRemaining(
                      operationEntry -> {
                        String methodKey = operationEntry.getKey();
                        if (!HTTP_METHODS.contains(methodKey.toLowerCase())) {
                          return;
                        }
                        String method = methodKey.toLowerCase();
                        JsonNode operation = operationEntry.getValue();
                        if (!operation.isObject()) {
                          return;
                        }
                        String operationId =
                            operation.hasNonNull("operationId")
                                ? operation.get("operationId").asText()
                                : synthesiseOperationId(method, path);

                        JsonNode responses = operation.get("responses");
                        if (responses != null) {
                          Map<String, JsonNode> responseMap = new HashMap<>();
                          responses
                              .fields()
                              .forEachRemaining(
                                  responseEntry ->
                                      responseMap.put(
                                          responseEntry.getKey(), responseEntry.getValue()));
                          apiOperations.put(operationId, responseMap);
                        }
                      });
            });

    return apiOperations;
  }

  /** Delegates to {@link io.spec0.mockserver.engine.openapi.OpenApiSpecSupport}. */
  static String synthesiseOperationId(String method, String path) {
    return io.spec0.mockserver.engine.openapi.OpenApiSpecSupport.synthesiseOperationId(
        method, path);
  }
}
