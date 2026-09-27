package io.spec0.mockserver.mockgen;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Generated responses must prefer example data written in the spec over random values. */
class MockResponseGeneratorExamplesTest {
  private static final YAMLMapper YAML = new YAMLMapper();
  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String BOOK_COMPONENTS =
      """
      components:
        schemas:
          Book:
            type: object
            required: [id, title]
            properties:
              id: { type: string, example: "b-1" }
              title: { type: string, example: "The Pragmatic Programmer" }
              author: { type: string, example: "Andy Hunt" }
      """;

  private static final String BOOKS_PATH =
      """
      paths:
        /books:
          get:
            operationId: listBooks
            responses:
              "200":
                description: ok
                content:
                  application/json:
                    schema: { type: array, items: { $ref: "#/components/schemas/Book" } }
      """;

  private static final String EXPECTED_BOOKS =
      "[{\"id\":\"b-1\",\"title\":\"The Pragmatic Programmer\",\"author\":\"Andy Hunt\"}]";

  /** Generates the body for {@code operationId}/{@code status} from a YAML spec. */
  private static JsonNode generate(String yaml, String operationId, String status)
      throws Exception {
    JsonNode root = YAML.readTree(yaml);
    Map<String, Map<String, JsonNode>> ops = new OpenApiParser().extractOperations(root);
    Object body =
        new MockResponseGenerator(root).generateMockData(ops.get(operationId).get(status));
    return JSON.valueToTree(body);
  }

  private static String spec(String version, String body) {
    return "openapi: " + version + "\ninfo: { title: t, version: '1' }\n" + body;
  }

  @Test
  void arrayOfRefSchema_usesPropertyExamples_openApi30() throws Exception {
    JsonNode body = generate(spec("3.0.3", BOOKS_PATH + BOOK_COMPONENTS), "listBooks", "200");
    assertEquals(JSON.readTree(EXPECTED_BOOKS), body);
  }

  @Test
  void arrayOfRefSchema_usesPropertyExamplesArray_openApi31() throws Exception {
    String components =
        """
        components:
          schemas:
            Book:
              type: object
              properties:
                id: { type: string, examples: ["b-1", "b-2"] }
                title: { type: [string, "null"], examples: ["The Pragmatic Programmer"] }
                author: { type: string, example: "Andy Hunt" }
        """;
    JsonNode body = generate(spec("3.1.0", BOOKS_PATH + components), "listBooks", "200");
    assertEquals(JSON.readTree(EXPECTED_BOOKS), body);
  }

  @Test
  void mediaTypeExample_winsOverSchemaExamples() throws Exception {
    String paths =
        """
        paths:
          /books:
            get:
              operationId: listBooks
              responses:
                "200":
                  description: ok
                  content:
                    application/json:
                      example: [{ id: "from-media" }]
                      schema: { type: array, items: { $ref: "#/components/schemas/Book" } }
        """;
    JsonNode body = generate(spec("3.0.3", paths + BOOK_COMPONENTS), "listBooks", "200");
    assertEquals(JSON.readTree("[{\"id\":\"from-media\"}]"), body);
  }

  @Test
  void mediaTypeNamedExamples_useFirstValue_includingRefs() throws Exception {
    String yaml =
        spec(
            "3.0.3",
            """
            paths:
              /books/{id}:
                get:
                  operationId: getBook
                  responses:
                    "200":
                      description: ok
                      content:
                        application/json:
                          examples:
                            first: { $ref: "#/components/examples/SampleBook" }
                            second: { value: { id: "second" } }
                          schema: { $ref: "#/components/schemas/Book" }
            components:
              examples:
                SampleBook:
                  value: { id: "named", title: "Refactoring" }
              schemas:
                Book:
                  type: object
                  properties:
                    id: { type: string, example: "b-1" }
            """);
    JsonNode body = generate(yaml, "getBook", "200");
    assertEquals(JSON.readTree("{\"id\":\"named\",\"title\":\"Refactoring\"}"), body);
  }

  @Test
  void schemaLevelExample_winsOverPropertyExamples() throws Exception {
    String components =
        """
        components:
          schemas:
            Book:
              type: object
              example: { id: "whole", title: "Whole-object example" }
              properties:
                id: { type: string, example: "b-1" }
        """;
    JsonNode body = generate(spec("3.0.3", BOOKS_PATH + components), "listBooks", "200");
    assertEquals(JSON.readTree("[{\"id\":\"whole\",\"title\":\"Whole-object example\"}]"), body);
  }

  @Test
  void arrayLevelExample_winsOverItems() throws Exception {
    String paths =
        """
        paths:
          /books:
            get:
              operationId: listBooks
              responses:
                "200":
                  description: ok
                  content:
                    application/json:
                      schema:
                        type: array
                        example: [{ id: "a" }, { id: "b" }]
                        items: { $ref: "#/components/schemas/Book" }
        """;
    JsonNode body = generate(spec("3.0.3", paths + BOOK_COMPONENTS), "listBooks", "200");
    assertEquals(JSON.readTree("[{\"id\":\"a\"},{\"id\":\"b\"}]"), body);
  }

  @Test
  void nestedObjectsAndArrays_useExamplesAtEveryLevel() throws Exception {
    String yaml =
        spec(
            "3.0.3",
            """
            paths:
              /orders/{id}:
                get:
                  operationId: getOrder
                  responses:
                    "200":
                      $ref: "#/components/responses/OrderResponse"
            components:
              responses:
                OrderResponse:
                  description: ok
                  content:
                    application/json:
                      schema: { $ref: "#/components/schemas/Order" }
              schemas:
                Order:
                  type: object
                  properties:
                    id: { type: string, example: "o-1" }
                    customer: { $ref: "#/components/schemas/Customer" }
                    lines:
                      type: array
                      items: { $ref: "#/components/schemas/Line" }
                Customer:
                  type: object
                  properties:
                    name: { type: string, example: "Ada" }
                    address:
                      type: object
                      properties:
                        city: { type: string, example: "London" }
                Line:
                  type: object
                  properties:
                    sku: { type: string, example: "SKU-1" }
                    quantity: { type: integer, example: 2 }
                    price: { type: number, example: 9.99 }
            """);
    JsonNode body = generate(yaml, "getOrder", "200");
    assertEquals(
        JSON.readTree(
            """
            {"id":"o-1",
             "customer":{"name":"Ada","address":{"city":"London"}},
             "lines":[{"sku":"SKU-1","quantity":2,"price":9.99}]}
            """),
        body);
  }

  @Test
  void allOf_mergesPartsAndKeepsPropertyExamples() throws Exception {
    String yaml =
        spec(
            "3.0.3",
            """
            paths:
              /ebooks/{id}:
                get:
                  operationId: getEbook
                  responses:
                    "200":
                      description: ok
                      content:
                        application/json:
                          schema:
                            allOf:
                              - $ref: "#/components/schemas/Book"
                              - type: object
                                properties:
                                  fileSize: { type: integer, example: 2048 }
            """
                + BOOK_COMPONENTS);
    JsonNode body = generate(yaml, "getEbook", "200");
    assertEquals(
        JSON.readTree(
            "{\"id\":\"b-1\",\"title\":\"The Pragmatic Programmer\",\"author\":\"Andy Hunt\","
                + "\"fileSize\":2048}"),
        body);
  }

  @Test
  void defaultThenEnumThenConst_areUsedWhenNoExample() throws Exception {
    String yaml =
        spec(
            "3.1.0",
            """
            paths:
              /status:
                get:
                  operationId: getStatus
                  responses:
                    "200":
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              withDefault: { type: string, enum: [a, b], default: b }
                              enumOnly: { type: string, enum: [first, second] }
                              constant: { const: fixed }
                              pick:
                                oneOf:
                                  - { type: string, example: "from-oneOf" }
                                  - { type: integer }
            """);
    JsonNode body = generate(yaml, "getStatus", "200");
    assertEquals(
        JSON.readTree(
            "{\"withDefault\":\"b\",\"enumOnly\":\"first\",\"constant\":\"fixed\","
                + "\"pick\":\"from-oneOf\"}"),
        body);
  }

  @Test
  void withoutExamples_stillGeneratesFormatAwareAndRandomValues() throws Exception {
    String yaml =
        spec(
            "3.0.3",
            """
            paths:
              /users:
                get:
                  operationId: listUsers
                  responses:
                    "200":
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              type: object
                              properties:
                                id: { type: string, format: uuid }
                                email: { type: string, format: email }
                                name: { type: string }
                                age: { type: integer }
                                score: { type: number }
                                active: { type: boolean }
            """);
    JsonNode user = generate(yaml, "listUsers", "200").get(0);
    assertDoesNotThrow(() -> UUID.fromString(user.get("id").asText()));
    assertTrue(user.get("email").asText().contains("@"));
    assertTrue(user.get("name").isTextual() && !user.get("name").asText().isEmpty());
    assertTrue(user.get("age").isInt());
    assertTrue(user.get("score").isNumber());
    assertTrue(user.get("active").isBoolean());
  }

  @Test
  void selfReferencingSchema_doesNotRecurseForever() throws Exception {
    String yaml =
        spec(
            "3.0.3",
            """
            paths:
              /tree:
                get:
                  operationId: getTree
                  responses:
                    "200":
                      description: ok
                      content:
                        application/json:
                          schema: { $ref: "#/components/schemas/Node" }
            components:
              schemas:
                Node:
                  type: object
                  properties:
                    name: { type: string, example: "root" }
                    children:
                      type: array
                      items: { $ref: "#/components/schemas/Node" }
            """);
    JsonNode body = generate(yaml, "getTree", "200");
    assertEquals("root", body.get("name").asText());
    assertTrue(body.get("children").isArray());
  }
}
