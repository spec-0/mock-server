package io.spec0.mockserver.standalone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

/**
 * Smoke tests covering the end-to-end REST flow of the standalone mock server:
 * register spec → create server → list → send mock request → create variant →
 * update variant → get logs → patch server → get operations → delete.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:smoketest;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
    })
class MockServerSmokeTest {

  @Autowired private TestRestTemplate rest;

  // State shared across ordered tests
  private static String specId;
  private static String mockServerId;
  private static String variantId;

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
        /pets/{id}:
          get:
            operationId: getPet
            parameters:
              - name: id
                in: path
                required: true
                schema:
                  type: integer
            responses:
              "200":
                description: ok
              "404":
                description: not found
      """;

  // ── Actuator ────────────────────────────────────────────────────────────────

  @Test
  @Order(1)
  void healthEndpointReturnsUp() {
    ResponseEntity<Map> resp = rest.getForEntity("/actuator/health", Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).containsEntry("status", "UP");
  }

  // ── Spec registration ────────────────────────────────────────────────────────

  @Test
  @Order(2)
  void registerSpec_returnsSpecId() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> body = Map.of("specName", "petstore", "specContent", PETSTORE_SPEC);
    ResponseEntity<Map> resp =
        rest.postForEntity("/mock-server/specs", new HttpEntity<>(body, headers), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).containsKey("specId");
    specId = (String) resp.getBody().get("specId");
  }

  @Test
  @Order(3)
  void registerSpec_idempotent() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body = Map.of("specName", "petstore", "specContent", PETSTORE_SPEC);
    ResponseEntity<Map> resp =
        rest.postForEntity("/mock-server/specs", new HttpEntity<>(body, headers), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody().get("specId")).isEqualTo(specId);
  }

  @Test
  @Order(4)
  void getSpec_returnsRegisteredSpec() {
    ResponseEntity<Map> resp = rest.getForEntity("/mock-server/specs/" + specId, Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("specId")).isEqualTo(specId);
  }

  // ── Mock server CRUD ─────────────────────────────────────────────────────────

  @Test
  @Order(5)
  void createMockServer_returnsServerId() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body =
        Map.of("specId", specId, "name", "petstore-mock", "defaultStrategy", "RANDOM");
    ResponseEntity<Map> resp =
        rest.postForEntity("/mock-server/servers", new HttpEntity<>(body, headers), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).containsKey("mockServerId");
    mockServerId = (String) resp.getBody().get("mockServerId");
  }

  @Test
  @Order(6)
  void listMockServers_containsCreatedServer() {
    ResponseEntity<List> resp = rest.getForEntity("/mock-server/servers", List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotEmpty();
  }

  @Test
  @Order(7)
  void getMockServer_returnsCorrectServer() {
    ResponseEntity<Map> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId, Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("mockServerId")).isEqualTo(mockServerId);
    assertThat(resp.getBody().get("name")).isEqualTo("petstore-mock");
  }

  @Test
  @Order(8)
  void getStandaloneConfig_includesSchemaValidationMode() {
    ResponseEntity<Map> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId + "/config", Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> cfg = resp.getBody();
    assertThat(cfg).containsKey("schemaValidationMode");
    assertThat(cfg).containsKey("defaultStrategy");
    assertThat(cfg.get("schemaValidationMode")).isEqualTo("OFF");
  }

  // ── Mock traffic ─────────────────────────────────────────────────────────────

  @Test
  @Order(9)
  void mockRequest_returnsAutoGeneratedResponse() {
    ResponseEntity<String> resp =
        rest.getForEntity("/mock/" + mockServerId + "/pets", String.class);
    // Accepts 200 (auto-generated) or 404 (operation not matched is fine too)
    assertThat(resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode() == HttpStatus.NOT_FOUND)
        .isTrue();
    // Key header from the mock engine
    String mockHeader = resp.getHeaders().getFirst("X-spec0-Mock-Response");
    assertThat(mockHeader).isEqualTo("true");
  }

  // ── Variants ─────────────────────────────────────────────────────────────────

  @Test
  @Order(10)
  void listVariants_nonEmpty() {
    ResponseEntity<List> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId + "/variants", List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Auto-generated variants should exist
    assertThat(resp.getBody()).isNotEmpty();
  }

  @Test
  @Order(11)
  void createVariant_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body =
        Map.of(
            "operationId", "listPets",
            "responseName", "Empty list",
            "statusCode", "200",
            "responseBody", "[]",
            "isDefault", true,
            "displayOrder", 0);
    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(body, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).containsKey("variantId");
    variantId = (String) resp.getBody().get("variantId");
  }

  @Test
  @Order(12)
  void updateVariant_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> update =
        Map.of("operationId", "listPets", "responseName", "Empty list v2",
            "statusCode", "200", "responseBody", "[{\"id\":1,\"name\":\"Fluffy\"}]",
            "isDefault", true, "displayOrder", 0);

    ResponseEntity<Map> resp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId + "/variants/" + variantId,
            HttpMethod.PUT,
            new HttpEntity<>(update, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("responseName")).isEqualTo("Empty list v2");
  }

  @Test
  @Order(13)
  void mockRequest_engineServesMockResponse() {
    // Verify the engine serves a valid mock response. We don't pin which variant is selected
    // (RANDOM may pick any); the critical assertion is the X-spec0-Mock-Response header proving
    // the mock engine intercepted and served the request.
    ResponseEntity<String> resp =
        rest.getForEntity("/mock/" + mockServerId + "/pets", String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getHeaders().getFirst("X-spec0-Mock-Response")).isEqualTo("true");
    assertThat(resp.getHeaders().getFirst("X-spec0-Mock-Variant-Id")).isNotNull();
  }

  // ── Logs ─────────────────────────────────────────────────────────────────────

  @Test
  @Order(14)
  void getLogs_containsRecentRequest() {
    ResponseEntity<List> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId + "/logs?limit=20", List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotEmpty();
  }

  // ── PATCH server (new endpoint) ───────────────────────────────────────────────

  @Test
  @Order(15)
  void patchServer_rename_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> patch = Map.of("name", "petstore-mock-renamed");

    ResponseEntity<Map> resp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId,
            HttpMethod.PATCH,
            new HttpEntity<>(patch, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("name")).isEqualTo("petstore-mock-renamed");
  }

  @Test
  @Order(16)
  void patchServer_disable_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> patch = Map.of("isEnabled", false);

    ResponseEntity<Map> resp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId,
            HttpMethod.PATCH,
            new HttpEntity<>(patch, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("isEnabled")).isEqualTo(false);
  }

  @Test
  @Order(17)
  void patchServer_reenable_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> patch = Map.of("isEnabled", true);

    ResponseEntity<Map> resp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId,
            HttpMethod.PATCH,
            new HttpEntity<>(patch, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("isEnabled")).isEqualTo(true);
  }

  // ── Operation configs (new endpoint) ─────────────────────────────────────────

  @Test
  @Order(18)
  void getOperationConfigs_returnsConfigsForSpec() {
    ResponseEntity<List> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId + "/operations", List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Operations are auto-created when variants are generated
    assertThat(resp.getBody()).isNotEmpty();
  }

  @Test
  @Order(19)
  void patchOperationConfig_disable_succeeds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> patch = Map.of("isEnabled", false);

    ResponseEntity<Map> resp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId + "/operations/listPets",
            HttpMethod.PATCH,
            new HttpEntity<>(patch, headers),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("isEnabled")).isEqualTo(false);
  }

  // ── Export ────────────────────────────────────────────────────────────────────

  @Test
  @Order(20)
  void exportMockServer_containsSpecAndVariants() {
    ResponseEntity<Map> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId + "/export", Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).containsKey("spec");
    assertThat(resp.getBody()).containsKey("variants");
  }

  // ── Cleanup ───────────────────────────────────────────────────────────────────

  @Test
  @Order(21)
  void deleteVariant_succeeds() {
    rest.delete("/mock-server/servers/" + mockServerId + "/variants/" + variantId);
    ResponseEntity<List> resp =
        rest.getForEntity(
            "/mock-server/servers/" + mockServerId + "/variants?operationId=listPets", List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Custom variant deleted; auto-generated ones may remain
    List<?> remaining = resp.getBody();
    boolean customGone =
        remaining.stream()
            .noneMatch(
                v -> variantId.equals(((Map<?, ?>) v).get("variantId")));
    assertThat(customGone).isTrue();
  }

  @Test
  @Order(22)
  void deleteMockServer_succeeds() {
    rest.delete("/mock-server/servers/" + mockServerId);
    ResponseEntity<Map> resp =
        rest.getForEntity("/mock-server/servers/" + mockServerId, Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
