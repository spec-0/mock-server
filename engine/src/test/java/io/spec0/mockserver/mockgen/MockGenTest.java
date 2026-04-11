package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class MockGenTest {
    private static final String SAMPLE_SPEC_PATH = "src/test/resources/sample-openapi.json";

    private MockResponseStore store;
    private MockResponseGenerator generator;
    private Map<String, Map<String, JsonNode>> operations;

    @BeforeEach
    public void setUp() throws IOException {
        // Load OpenAPI spec file
        File specFile = new File(SAMPLE_SPEC_PATH);
        assertTrue(specFile.exists(), "OpenAPI spec file does not exist: " + SAMPLE_SPEC_PATH);

        // Initialize OpenAPI Parser
        OpenApiParser parser = new OpenApiParser();
        JsonNode rootNode = parser.parseSpec(specFile);

        // Extract API operations (operationId -> responseCode -> schema)
        operations = parser.extractOperations(rootNode);

        // Initialize Mock Response Generator
        generator = new MockResponseGenerator(rootNode);
        store = new MockResponseStore();

        // Generate mock responses for each operation and response code
        operations.forEach((operationId, responses) -> responses.forEach((responseCode, responseSchema) -> {
            Object mockData = generator.generateMockData(responseSchema);
            store.storeMockResponse(operationId, responseCode, mockData);
        }));
    }

    @Test
    public void testMockResponsesGenerated() {
        // Ensure at least one operation exists
        assertFalse(operations.isEmpty(), "No operations found in OpenAPI spec");

        // Ensure that mock responses are stored
        assertFalse(store.getAllMockResponses().isEmpty(), "Mock responses store is empty");

        // Verify a specific operation (e.g., "getUsers") exists
        String operationId = "getUsers";
        assertTrue(store.getAllMockResponses().containsKey(operationId), "Operation " + operationId + " not found in mock responses");

        // Verify response codes exist for the operation
        Map<String, Object> responses = store.getAllMockResponses().get(operationId);
        assertNotNull(responses, "No responses found for operation: " + operationId);
        assertFalse(responses.isEmpty(), "No response codes found for operation: " + operationId);
    }

    @Test
    public void testMockResponseSerialization() throws IOException {
        // Initialize Jackson ObjectMapper for JSON formatting
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print JSON

        // Serialize entire mock response store to formatted JSON
        String formattedJson = objectMapper.writeValueAsString(store.getAllMockResponses());
        assertNotNull(formattedJson, "Serialized mock response is null");
        assertFalse(formattedJson.isEmpty(), "Serialized mock response is empty");

        System.out.println("\nGenerated Mock Responses:");
        System.out.println(formattedJson);
    }

    @Test
    public void testFetchMockResponse() {
        String operationId = "getUsers"; // Example operation ID
        String responseCode = "200"; // Example response code

        Object mockResponse = store.getMockResponse(operationId, responseCode);
        assertNotNull(mockResponse, "No mock response found for operation: " + operationId + " with response code: " + responseCode);

        // Print formatted response
        System.out.println("\nMock Response for " + operationId + " (Response Code " + responseCode + "):");
        System.out.println(mockResponse);
    }
}
