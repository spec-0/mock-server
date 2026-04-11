package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MockResponseGenerator {
    private final Faker faker = new Faker();
    private final JsonNode components; // Store OpenAPI components

    public MockResponseGenerator(JsonNode rootNode) {
        this.components = rootNode.has("components") && rootNode.get("components").has("schemas")
                ? rootNode.get("components").get("schemas")
                : null;
    }

    public MockResponseGenerator(String openApiSpecString) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(openApiSpecString);
        this.components = rootNode.has("components") && rootNode.get("components").has("schemas")
                ? rootNode.get("components").get("schemas")
                : null;
    }

    public Object generateMockData(JsonNode schema) {
        if (schema == null) return null;

        // Handle $ref: Resolve reference
        if (schema.has("$ref")) {
            String refPath = schema.get("$ref").asText();
            return resolveRef(refPath);
        }

        // Handle nested "content" → "*/*" → "schema"
        if (schema.has("content")) {
            JsonNode contentNode = schema.get("content");
            Iterator<JsonNode> contentSchemas = contentNode.elements();
            while (contentSchemas.hasNext()) {
                JsonNode contentEntry = contentSchemas.next();
                if (contentEntry.has("schema")) {
                    return generateMockData(contentEntry.get("schema"));
                }
            }
        }

        // Handle direct schema cases
        if (!schema.has("type")) return null;
        String type = schema.get("type").asText();

        return switch (type) {
            case "string" -> generateStringMock(schema);
            case "integer" -> faker.number().numberBetween(1, 1000);
            case "boolean" -> faker.bool().bool();
            case "array" -> generateArrayMock(schema);
            case "object" -> generateObjectMock(schema);
            default -> null;
        };
    }

    private String generateStringMock(JsonNode schema) {
        if (schema.has("format")) {
            String format = schema.get("format").asText();
            return switch (format) {
                case "uuid" -> faker.internet().uuid();
                case "email" -> faker.internet().emailAddress();
                case "date-time" -> faker.date().birthday().toInstant().toString();
                case "ipv4" -> faker.internet().ipV4Address();
                default -> faker.lorem().word();
            };
        }
        return faker.lorem().word();
    }

    private Object generateArrayMock(JsonNode schema) {
        if (schema.has("items")) {
            return new Object[]{generateMockData(schema.get("items"))};
        }
        return new Object[0];
    }

    private Map<String, Object> generateObjectMock(JsonNode schema) {
        Map<String, Object> mockObject = new HashMap<>();
        if (schema.has("properties")) {
            schema.get("properties").fields().forEachRemaining(field -> mockObject.put(field.getKey(), generateMockData(field.getValue())));
        }
        return mockObject;
    }

    // Resolve $ref paths like "#/components/schemas/Api"
    private Object resolveRef(String refPath) {
        if (components == null || !refPath.startsWith("#/components/schemas/")) return null;
        String schemaName = refPath.replace("#/components/schemas/", "");
        JsonNode refSchema = components.get(schemaName);
        return refSchema != null ? generateMockData(refSchema) : null;
    }
}
