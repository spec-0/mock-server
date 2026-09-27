package io.spec0.mockserver.engine.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockResponseStrategy;
import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.openapi.OpenApiSpecSupport;
import io.spec0.mockserver.engine.spi.ApiSpecLookup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Creating a mock server must produce default responses built from the spec's example data, for
 * both OpenAPI 3.0 and 3.1 documents (the spec goes through a parse / re-serialise step first).
 */
class DefaultMockServerServiceExamplesTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  private static String bookSpec(String version) {
    String exampleKeyword = version.startsWith("3.1") ? "examples: [%s]" : "example: %s";
    String type = version.startsWith("3.1") ? "[string, \"null\"]" : "string";
    return """
        openapi: %s
        info: { title: Books, version: "1.0.0" }
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
        components:
          schemas:
            Book:
              type: object
              required: [id, title]
              properties:
                id: { type: string, %s }
                title: { type: %s, %s }
                author: { type: string, example: "Andy Hunt" }
        """
        .formatted(
            version,
            exampleKeyword.formatted("\"b-1\""),
            type,
            exampleKeyword.formatted("\"The Pragmatic Programmer\""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"3.0.3", "3.1.0"})
  void createMockServer_defaultVariantUsesPropertyExamples(String version) throws Exception {
    String yaml = bookSpec(version);
    UUID specId = UUID.randomUUID();
    InMemoryPersistence persistence = new InMemoryPersistence();
    ApiSpecLookup lookup =
        id ->
            id.equals(specId)
                ? Optional.of(new ApiSpecSnapshot(specId, "books", yaml, "hash", "1.0.0"))
                : Optional.empty();
    List<MockServerOperation> ops = OpenApiSpecSupport.extractOperations(specId, yaml);
    persistence.operationsBySpec.put(specId, ops);

    MockServer saved =
        new DefaultMockServerService(persistence, lookup)
            .createMockServer(specId, "mock", MockResponseStrategy.RANDOM);

    List<MockResponseVariant> variants =
        persistence.findVariantsByMockServerIdOrderByDisplayOrder(saved.getMockServerId());
    assertEquals(1, variants.size());
    JsonNode body = JSON.readTree(variants.get(0).getResponseBody());
    assertEquals(
        JSON.readTree(
            "[{\"id\":\"b-1\",\"title\":\"The Pragmatic Programmer\",\"author\":\"Andy Hunt\"}]"),
        body);
  }
}
