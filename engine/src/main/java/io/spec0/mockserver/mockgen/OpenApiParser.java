package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenApiParser {
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();

    public JsonNode parseSpec(File file) throws IOException {
        if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml") || file.getName().endsWith(".json")) {

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

        paths.fields().forEachRemaining(pathEntry -> {
            JsonNode pathItem = pathEntry.getValue();
            pathItem.fields().forEachRemaining(operationEntry -> {
                String method = operationEntry.getKey();
                JsonNode operation = operationEntry.getValue();
                String operationId = operation.has("operationId") ? operation.get("operationId").asText() : method + "_" + pathEntry.getKey();

                JsonNode responses = operation.get("responses");
                if (responses != null) {
                    Map<String, JsonNode> responseMap = new HashMap<>();
                    responses.fields().forEachRemaining(responseEntry -> responseMap.put(responseEntry.getKey(), responseEntry.getValue()));
                    apiOperations.put(operationId, responseMap);
                }
            });
        });

        return apiOperations;
    }
}
