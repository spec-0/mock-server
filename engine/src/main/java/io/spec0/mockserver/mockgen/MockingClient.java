package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MockingClient {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public String generateMockFromFile(File specFile) throws IOException {
    String specContent = Files.readString(specFile.toPath());
    return generateMockFromString(specContent);
  }

  public String generateMockFromString(String specContent) {
    JsonNode rootNode;
    try {
      rootNode = objectMapper.readTree(specContent);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return generateMockFromString(rootNode);
  }

  public String generateMockFromString(JsonNode rootNode) {
    try {
      MockResponseGenerator generator = new MockResponseGenerator(rootNode);
      OpenApiParser parser = new OpenApiParser();
      Map<String, Map<String, JsonNode>> operations = parser.extractOperations(rootNode);
      Map<String, Object> mockResponses = new HashMap<>();

      operations.forEach(
          (operationId, responses) -> {
            Map<String, Object> responseVariants = new HashMap<>();
            responses.forEach(
                (statusCode, responseSchema) ->
                    responseVariants.put(statusCode, generator.generateMockData(responseSchema)));
            mockResponses.put(operationId, responseVariants);
          });

      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mockResponses);
    } catch (Exception e) {
      return "{\"error\": \"Failed to generate mock responses\"}";
    }
  }
}
