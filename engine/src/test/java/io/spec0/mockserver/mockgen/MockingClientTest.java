package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MockingClientTest {
    private MockingClient mockingClient;
    private ObjectMapper objectMapper;


    private static final String SAMPLE_OPENAPI_SPEC = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test API", "version": "1.0.0" },
              "paths": {
                "/users": {
                  "get": {
                    "operationId": "getUsers",
                    "responses": {
                      "200": {
                        "description": "OK",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": { "type": "string" },
                                "name": { "type": "string" }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }""";

    @BeforeEach
    void setUp() {
        mockingClient = new MockingClient();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGenerateMockFromString() {

        try {
            String userApiJson = readFileFromResources("sample-openapi.json");

            String mockResponse = mockingClient.generateMockFromString(userApiJson);

            assertNotNull(mockResponse, "Mock response should not be null");

            try {
                JsonNode jsonResponse = objectMapper.readTree(mockResponse);
                assertTrue(jsonResponse.has("getUsers"), "Response should contain operationId 'getUsers'");
                assertTrue(jsonResponse.get("getUsers").has("200"), "Response should contain status 200");
                assertTrue(jsonResponse.get("getUsers").get("200").get("users").get(0).has("id"), "Mock should have 'id' field");
                assertTrue(jsonResponse.get("getUsers").get("200").get("users").get(0).has("name"), "Mock should have 'name' field");
            } catch (IOException e) {
                fail("Failed to parse JSON output");
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGenerateMockFromFile() throws IOException {
        // Create a temporary OpenAPI file
        File tempFile = File.createTempFile("openapi", ".json");
        Files.write(tempFile.toPath(), SAMPLE_OPENAPI_SPEC.getBytes());

        String mockResponse = mockingClient.generateMockFromFile(tempFile);

        assertNotNull(mockResponse, "Mock response should not be null");

        JsonNode jsonResponse = objectMapper.readTree(mockResponse);
        assertTrue(jsonResponse.has("getUsers"), "Response should contain operationId 'getUsers'");

        // Clean up
        tempFile.delete();
    }

    @Test
    void testHandleInvalidJsonInput() {
        String invalidSpec = "{ invalid json }";  // Malformed JSON

        assertThrows(RuntimeException.class, () -> mockingClient.generateMockFromString(invalidSpec));

    }

    public static String readFileFromResources(String fileName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Path filePath = Path.of(Objects.requireNonNull(classLoader.getResource(fileName)).toURI());
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
}
