package io.spec0.mockserver.engine.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockOperationConfig;
import io.spec0.mockserver.engine.model.MockRequestLog;
import io.spec0.mockserver.engine.model.MockResponseStrategy;
import io.spec0.mockserver.engine.model.MockResponseVariant;
import io.spec0.mockserver.engine.model.MockServer;
import io.spec0.mockserver.engine.model.MockServerConfig;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.openapi.OpenApiSpecSupport;
import io.spec0.mockserver.engine.spi.ApiSpecLookup;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
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

  /** Minimal in-memory persistence covering only what the create-mock path needs. */
  private static final class InMemoryPersistence implements MockServerPersistencePort {
    private final AtomicLong ids = new AtomicLong();
    private final List<MockResponseVariant> variants = new ArrayList<>();
    private final java.util.Map<UUID, List<MockServerOperation>> operationsBySpec =
        new java.util.HashMap<>();

    @Override
    public MockServer saveMockServer(MockServer server) {
      if (server.getMockServerId() == null) {
        server.setMockServerId(UUID.randomUUID());
      }
      return server;
    }

    @Override
    public MockServerConfig saveMockServerConfig(MockServerConfig config) {
      if (config.getConfigId() == null) {
        config.setConfigId(UUID.randomUUID());
      }
      return config;
    }

    @Override
    public MockResponseVariant saveVariant(MockResponseVariant variant) {
      if (variant.getVariantId() == null) {
        variant.setVariantId(UUID.randomUUID());
      }
      variants.add(variant);
      return variant;
    }

    @Override
    public List<MockResponseVariant> findVariantsByMockServerIdOrderByDisplayOrder(
        UUID mockServerId) {
      return variants.stream().filter(v -> v.getMockServerId().equals(mockServerId)).toList();
    }

    @Override
    public List<MockServerOperation> findOperationsBySpecId(UUID specId) {
      return operationsBySpec.getOrDefault(specId, List.of());
    }

    @Override
    public MockOperationConfig saveOperationConfig(MockOperationConfig config) {
      return config;
    }

    // ── Unused by the create-mock path ────────────────────────────────────────
    @Override
    public Optional<MockServer> findMockServerById(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MockServer> findAllMockServers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MockServer> findMockServersBySpecId(UUID specId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMockServerById(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MockServerConfig> findConfigByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConfigById(UUID configId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MockResponseVariant> findVariantById(UUID variantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MockResponseVariant> findVariantsByMockServerIdAndOperationIdOrderByDisplayOrder(
        UUID mockServerId, String operationId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MockResponseVariant> findFirstDefaultVariant(
        UUID mockServerId, String operationId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long countVariantsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long countVariantsByMockServerIdAndOperationId(UUID mockServerId, String operationId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVariantById(UUID variantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVariantsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MockOperationConfig> findOperationConfigsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MockOperationConfig> findOperationConfigByMockServerIdAndOperationId(
        UUID mockServerId, String operationId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteOperationConfigsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void saveRequestLog(MockRequestLog log) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MockRequestLog> findRecentLogsByMockServerId(UUID mockServerId, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteLogsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEnvVarsByMockServerId(UUID mockServerId) {
      throw new UnsupportedOperationException();
    }
  }
}
