package io.spec0.mockserver.mockgen;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenApiParserTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final OpenApiParser parser = new OpenApiParser();

  @Test
  void extractOperations_usesSameSynthesisedIdAsPlatformWhenOperationIdMissing() throws Exception {
    String spec =
        """
        {
          "openapi": "3.0.0",
          "info": { "title": "T", "version": "1" },
          "paths": {
            "/v1/users": {
              "get": {
                "responses": {
                  "200": { "description": "ok" }
                }
              }
            }
          }
        }""";

    JsonNode root = mapper.readTree(spec);
    Map<String, Map<String, JsonNode>> ops = parser.extractOperations(root);

    String expectedId = PlatformStyleSynthesis.synthesiseOperationId("GET", "/v1/users");
    assertTrue(ops.containsKey(expectedId), "Expected key " + expectedId + " got " + ops.keySet());
  }

  @Test
  void extractOperations_ignoresPathItemParametersAndSummary() throws Exception {
    String spec =
        """
        {
          "openapi": "3.0.0",
          "info": { "title": "T", "version": "1" },
          "paths": {
            "/pets": {
              "summary": "Pets",
              "parameters": [],
              "get": {
                "operationId": "listPets",
                "responses": { "200": { "description": "ok" } }
              }
            }
          }
        }""";

    JsonNode root = mapper.readTree(spec);
    Map<String, Map<String, JsonNode>> ops = parser.extractOperations(root);

    assertEquals(1, ops.size());
    assertTrue(ops.containsKey("listPets"));
  }

  /** Mirrors {@code PlatformApiSpecService#synthesiseOperationId} for test assertion only. */
  private static final class PlatformStyleSynthesis {
    static String synthesiseOperationId(String method, String path) {
      return method.toLowerCase()
          + "_"
          + path.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }
  }
}
