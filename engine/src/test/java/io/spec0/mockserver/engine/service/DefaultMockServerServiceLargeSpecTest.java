package io.spec0.mockserver.engine.service;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Test;

/**
 * Proves that creating a mock server from a large OpenAPI spec (one that exceeds SnakeYAML's
 * default 3 MB per-document code-point limit) succeeds and that default variants are generated from
 * the parsed spec — i.e. {@code toJson} parsed the spec rather than silently falling back to the
 * raw spec, which would have left generated variants without bodies.
 */
class DefaultMockServerServiceLargeSpecTest {

  /** Builds a valid OpenAPI 3.0 YAML document above {@code minCodePoints} with N GET operations. */
  private static String largeValidYamlSpec(int minCodePoints) {
    StringBuilder sb = new StringBuilder(minCodePoints + 4096);
    sb.append("openapi: 3.0.0\n")
        .append("info:\n")
        .append("  title: Large Spec\n")
        .append("  version: '1.0.0'\n")
        .append("paths:\n");
    int i = 0;
    while (sb.length() < minCodePoints) {
      sb.append("  /resource").append(i).append(":\n");
      sb.append("    get:\n");
      sb.append("      operationId: getResource").append(i).append("\n");
      sb.append("      summary: Padding to inflate the document size for resource ").append(i);
      sb.append(" well past SnakeYAML's default 3 MB code-point limit so the bug would trigger\n");
      sb.append("      responses:\n");
      sb.append("        '200':\n");
      sb.append("          description: ok\n");
      sb.append("          content:\n");
      sb.append("            application/json:\n");
      sb.append("              schema:\n");
      sb.append("                type: object\n");
      sb.append("                properties:\n");
      sb.append("                  id:\n");
      sb.append("                    type: integer\n");
      sb.append("                  name:\n");
      sb.append("                    type: string\n");
      i++;
    }
    return sb.toString();
  }

  @Test
  void createMockServer_largeSpec_generatesVariantsFromParsedSpec() {
    // ~4 MB valid OpenAPI YAML — comfortably above the 3 MB SnakeYAML default.
    String bigYaml = largeValidYamlSpec(4 * 1024 * 1024);
    assertTrue(
        bigYaml.codePointCount(0, bigYaml.length()) > 3 * 1024 * 1024,
        "test spec must exceed SnakeYAML's default limit");

    UUID specId = UUID.randomUUID();
    InMemoryPersistence persistence = new InMemoryPersistence();
    ApiSpecLookup lookup =
        id ->
            id.equals(specId)
                ? Optional.of(new ApiSpecSnapshot(specId, "big", bigYaml, "hash", "1.0.0"))
                : Optional.empty();

    // Use the engine's own operation extractor so operationIds match what mock generation emits.
    List<MockServerOperation> ops = OpenApiSpecSupport.extractOperations(specId, bigYaml);
    assertFalse(ops.isEmpty(), "large spec should yield operations");
    persistence.operationsBySpec.put(specId, ops);

    DefaultMockServerService service = new DefaultMockServerService(persistence, lookup);

    MockServer saved = service.createMockServer(specId, "mock", MockResponseStrategy.RANDOM);
    assertNotNull(saved.getMockServerId());

    List<MockResponseVariant> variants =
        persistence.findVariantsByMockServerIdOrderByDisplayOrder(saved.getMockServerId());
    assertEquals(ops.size(), variants.size(), "one default variant per operation");
    assertTrue(variants.stream().allMatch(v -> Boolean.TRUE.equals(v.getIsGenerated())));

    // The load-bearing assertion: the spec parsed (not the raw-spec fallback), so mock generation
    // produced response bodies for the operations. Against the pre-fix code the large YAML would
    // fail to parse, toJson would return the raw YAML, MockingClient could not read it as JSON, and
    // every variant body would be null.
    long withBody =
        variants.stream()
            .filter(v -> v.getResponseBody() != null && !v.getResponseBody().isBlank())
            .count();
    assertTrue(
        withBody > 0,
        "at least one generated variant must have a body, proving the large spec was parsed");
  }
}
