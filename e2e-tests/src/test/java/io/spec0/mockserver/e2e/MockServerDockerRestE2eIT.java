package io.spec0.mockserver.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Hits a real HTTP base URL (Docker or elsewhere). Skipped unless {@code -Dmock.server.baseUrl=} is
 * set — {@code scripts/e2e-against-docker.sh} sets this after starting the container.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "mock.server.baseUrl", matches = "https?://.+")
class MockServerDockerRestE2eIT {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private static final String BASE = System.getProperty("mock.server.baseUrl").replaceAll("/$", "");

  private static String specId;
  private static String mockServerId;

  private static final String PETSTORE_SPEC =
      """
      openapi: 3.0.0
      info:
        title: Petstore
        version: 1.0.0
      paths:
        /pets:
          get:
            operationId: listPets
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
                          id:
                            type: integer
                          name:
                            type: string
      """;

  @Test
  @Order(1)
  void actuatorHealthIsUp() throws Exception {
    HttpResponse<String> resp = get("/actuator/health");
    assertThat(resp.statusCode()).isEqualTo(200);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("status").asText()).isEqualTo("UP");
  }

  @Test
  @Order(2)
  void registerSpec_returnsCreated() throws Exception {
    String body =
        MAPPER.writeValueAsString(Map.of("specName", "e2e-petstore", "specContent", PETSTORE_SPEC));
    HttpResponse<String> resp = postJson("/mock-server/specs", body);
    assertThat(resp.statusCode()).isEqualTo(201);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.has("specId")).isTrue();
    specId = root.get("specId").asText();
  }

  @Test
  @Order(3)
  void createMockServer_returnsCreated() throws Exception {
    String body =
        MAPPER.writeValueAsString(
            Map.of("specId", specId, "name", "e2e-mock", "defaultStrategy", "RANDOM"));
    HttpResponse<String> resp = postJson("/mock-server/servers", body);
    assertThat(resp.statusCode()).isEqualTo(201);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.has("mockServerId")).isTrue();
    mockServerId = root.get("mockServerId").asText();
  }

  @Test
  @Order(4)
  void mockRequest_returnsMockHeaders() throws Exception {
    HttpResponse<String> resp = get("/mock/" + mockServerId + "/pets");
    assertThat(resp.statusCode()).isBetween(200, 299);
    assertThat(resp.headers().firstValue("X-spec0-Mock-Response")).hasValue("true");
  }

  @Test
  @Order(5)
  void listVariants_returnsOk() throws Exception {
    HttpResponse<String> resp = get("/mock-server/servers/" + mockServerId + "/variants");
    assertThat(resp.statusCode()).isEqualTo(200);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.isArray()).isTrue();
    assertThat(root.size()).isGreaterThan(0);
  }

  @Test
  @Order(6)
  void getSpec_returnsRegisteredDocument() throws Exception {
    HttpResponse<String> resp = get("/mock-server/specs/" + specId);
    assertThat(resp.statusCode()).isEqualTo(200);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("specId").asText()).isEqualTo(specId);
    assertThat(root.path("specName").asText()).isEqualTo("e2e-petstore");
  }

  @Test
  @Order(7)
  void registerSpec_missingFields_returnsBadRequestEmptyBody() throws Exception {
    String body = MAPPER.writeValueAsString(Map.of("specName", "only-name"));
    HttpResponse<String> resp = postJson("/mock-server/specs", body);
    assertThat(resp.statusCode()).isEqualTo(400);
    assertThat(resp.body()).isBlank();
  }

  @Test
  @Order(8)
  void registerSpec_malformedJson_returnsBadRequest() throws Exception {
    HttpResponse<String> resp = postJson("/mock-server/specs", "{not json");
    assertThat(resp.statusCode()).isEqualTo(400);
  }

  @Test
  @Order(9)
  void getSpec_unknownId_returnsNotFound() throws Exception {
    UUID random = UUID.randomUUID();
    HttpResponse<String> resp = get("/mock-server/specs/" + random);
    assertThat(resp.statusCode()).isEqualTo(404);
    assertThat(resp.body()).isBlank();
  }

  @Test
  @Order(10)
  void createMockServer_unknownSpec_returnsNotFoundEnvelope() throws Exception {
    UUID missing = UUID.randomUUID();
    String body =
        MAPPER.writeValueAsString(
            Map.of("specId", missing.toString(), "name", "orphan", "defaultStrategy", "RANDOM"));
    HttpResponse<String> resp = postJson("/mock-server/servers", body);
    assertThat(resp.statusCode()).isEqualTo(404);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("error").asText()).isEqualTo("not_found");
    assertThat(root.path("message").asText()).contains("Spec not found");
  }

  @Test
  @Order(11)
  void createMockServer_invalidSpecId_returnsBadRequestEnvelope() throws Exception {
    String body =
        MAPPER.writeValueAsString(
            Map.of("specId", "not-a-uuid", "name", "bad-id", "defaultStrategy", "RANDOM"));
    HttpResponse<String> resp = postJson("/mock-server/servers", body);
    assertThat(resp.statusCode()).isEqualTo(400);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("error").asText()).isEqualTo("invalid_argument");
  }

  @Test
  @Order(12)
  void listVariants_unknownServer_returnsNotFoundEmptyBody() throws Exception {
    HttpResponse<String> resp = get("/mock-server/servers/" + UUID.randomUUID() + "/variants");
    assertThat(resp.statusCode()).isEqualTo(404);
    assertThat(resp.body()).isBlank();
  }

  @Test
  @Order(13)
  void mockRequest_unknownServer_returnsJsonError() throws Exception {
    HttpResponse<String> resp = get("/mock/" + UUID.randomUUID() + "/pets");
    assertThat(resp.statusCode()).isEqualTo(404);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("error").asText()).isEqualTo("mock_server_not_found");
    assertThat(resp.headers().firstValue("X-spec0-Mock-Response")).hasValue("true");
  }

  @Test
  @Order(14)
  void mockRequest_unknownPath_returnsNoVariantsError() throws Exception {
    HttpResponse<String> resp = get("/mock/" + mockServerId + "/does-not-exist-in-spec");
    assertThat(resp.statusCode()).isEqualTo(404);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("error").asText()).isEqualTo("no_variants_for_operation");
    assertThat(root.path("operationId").asText()).isNotBlank();
  }

  @Test
  @Order(15)
  void mockOptions_returnsNoContentWithCors() throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/mock/" + mockServerId + "/pets"))
            .timeout(Duration.ofSeconds(30))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(204);
    assertThat(resp.headers().firstValue("Access-Control-Allow-Origin")).hasValue("*");
    assertThat(resp.headers().firstValue("Access-Control-Allow-Methods").orElse(""))
        .contains("GET");
  }

  @Test
  @Order(16)
  void mockRequest_whenServerDisabled_returns503ThenRecover() throws Exception {
    HttpResponse<String> patchOff =
        patchJson("/mock-server/servers/" + mockServerId, "{\"isEnabled\":false}");
    assertThat(patchOff.statusCode()).isEqualTo(200);

    HttpResponse<String> disabled = get("/mock/" + mockServerId + "/pets");
    assertThat(disabled.statusCode()).isEqualTo(503);
    JsonNode err = MAPPER.readTree(disabled.body());
    assertThat(err.path("error").asText()).isEqualTo("mock_server_disabled");

    HttpResponse<String> patchOn =
        patchJson("/mock-server/servers/" + mockServerId, "{\"isEnabled\":true}");
    assertThat(patchOn.statusCode()).isEqualTo(200);

    HttpResponse<String> again = get("/mock/" + mockServerId + "/pets");
    assertThat(again.statusCode()).isBetween(200, 299);
  }

  @Test
  @Order(17)
  void registerSpec_sameNameAndContent_isIdempotent_returnsSameSpecId() throws Exception {
    String body =
        MAPPER.writeValueAsString(Map.of("specName", "e2e-petstore", "specContent", PETSTORE_SPEC));
    HttpResponse<String> resp = postJson("/mock-server/specs", body);
    assertThat(resp.statusCode()).isEqualTo(201);
    JsonNode root = MAPPER.readTree(resp.body());
    assertThat(root.path("specId").asText()).isEqualTo(specId);
  }

  private static HttpResponse<String> get(String path) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
    return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> postJson(String path, String jsonBody) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE + path))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> patchJson(String path, String jsonBody) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE + path))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
  }
}
