package io.spec0.mockserver.standalone;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * Integration tests that drive the full HTTP stack to verify schema validation (WARN / STRICT)
 * rejects variants whose response bodies violate the OpenAPI schema.
 *
 * <p>Uses a dedicated in-memory H2 database to avoid state leaking between test classes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:schemavalidtest;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
    })
class SchemaValidationIntegrationTest {

  @Autowired private TestRestTemplate rest;

  private static final String SPEC_WITH_REQUIRED =
      """
      openapi: 3.0.0
      info:
        title: Validation Test API
        version: 1.0.0
      paths:
        /items:
          post:
            operationId: createItem
            requestBody:
              required: true
              content:
                application/json:
                  schema:
                    type: object
                    required:
                      - name
                    properties:
                      name:
                        type: string
            responses:
              "201":
                description: created
                content:
                  application/json:
                    schema:
                      type: object
                      required:
                        - id
                        - name
                      properties:
                        id:
                          type: string
                        name:
                          type: string
      """;

  private String specId;
  private String mockServerId;

  private HttpHeaders json() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  @BeforeEach
  void setUpServerWithStrictMode() {
    // Register spec
    ResponseEntity<Map> specResp =
        rest.postForEntity(
            "/mock-server/specs",
            new HttpEntity<>(
                Map.of("specName", "validation-test", "specContent", SPEC_WITH_REQUIRED), json()),
            Map.class);
    assertThat(specResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    specId = (String) specResp.getBody().get("specId");

    // Create mock server
    ResponseEntity<Map> serverResp =
        rest.postForEntity(
            "/mock-server/servers",
            new HttpEntity<>(
                Map.of(
                    "specId",
                    specId,
                    "name",
                    "validation-test-server",
                    "defaultStrategy",
                    "RANDOM"),
                json()),
            Map.class);
    assertThat(serverResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    mockServerId = (String) serverResp.getBody().get("mockServerId");
  }

  // ── STRICT mode ────────────────────────────────────────────────────────────

  @Test
  void strict_validBody_variantSaved() {
    enableValidation("STRICT");

    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "ok",
                    "statusCode", "201",
                    "responseBody", "{\"id\":\"1\",\"name\":\"widget\"}",
                    "isDefault", true,
                    "displayOrder", 0),
                json()),
            Map.class);

    assertThat(resp.getStatusCode())
        .as("STRICT mode with valid body must save (201 Created)")
        .isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void strict_missingRequiredField_variantRejected() {
    enableValidation("STRICT");

    // Body is missing required 'id' and 'name' fields
    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "bad",
                    "statusCode", "201",
                    "responseBody", "{\"wrong\":1}",
                    "isDefault", false,
                    "displayOrder", 1),
                json()),
            Map.class);

    assertThat(resp.getStatusCode())
        .as("STRICT mode with missing required fields must reject (400 Bad Request)")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resp.getBody()).containsKey("error");
    assertThat(resp.getBody().get("error").toString())
        .isEqualTo("response_schema_validation_failed");
  }

  @Test
  void strict_wrongType_variantRejected() {
    enableValidation("STRICT");

    // 'id' and 'name' must be strings; sending integers violates the schema
    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "bad-types",
                    "statusCode", "201",
                    "responseBody", "{\"id\":1,\"name\":2}",
                    "isDefault", false,
                    "displayOrder", 2),
                json()),
            Map.class);

    assertThat(resp.getStatusCode())
        .as("STRICT mode with wrong field types must reject (400 Bad Request)")
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  // ── WARN mode ─────────────────────────────────────────────────────────────

  @Test
  void warn_missingRequiredField_variantSaved_withLoggedWarning() {
    enableValidation("WARN");

    // In WARN mode, invalid bodies are logged but not rejected
    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "warn-variant",
                    "statusCode", "201",
                    "responseBody", "{\"wrong\":1}",
                    "isDefault", false,
                    "displayOrder", 3),
                json()),
            Map.class);

    assertThat(resp.getStatusCode())
        .as("WARN mode with invalid body must still save (201 Created) — warning is logged only")
        .isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void warn_invalidBody_responseContainsWarnings() {
    enableValidation("WARN");

    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "warn-warns",
                    "statusCode", "201",
                    "responseBody", "{\"wrong\":1}",
                    "isDefault", false,
                    "displayOrder", 5),
                json()),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody())
        .as("WARN mode with schema violations must include 'validationWarnings' in response")
        .containsKey("validationWarnings");
    @SuppressWarnings("unchecked")
    List<String> warnings = (List<String>) resp.getBody().get("validationWarnings");
    assertThat(warnings).isNotEmpty();
  }

  // ── OFF mode ───────────────────────────────────────────────────────────────

  @Test
  void off_missingRequiredField_variantSaved_noValidation() {
    // Default is OFF — no validation at all
    ResponseEntity<Map> resp =
        rest.postForEntity(
            "/mock-server/servers/" + mockServerId + "/variants",
            new HttpEntity<>(
                Map.of(
                    "operationId", "createItem",
                    "responseName", "off-variant",
                    "statusCode", "201",
                    "responseBody", "{\"anything\":true}",
                    "isDefault", false,
                    "displayOrder", 4),
                json()),
            Map.class);

    assertThat(resp.getStatusCode())
        .as("OFF mode must allow any body without validation")
        .isEqualTo(HttpStatus.CREATED);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private void enableValidation(String mode) {
    ResponseEntity<Map> patchResp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId + "/config",
            HttpMethod.PATCH,
            new HttpEntity<>(Map.of("schemaValidationMode", mode), json()),
            Map.class);
    assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(patchResp.getBody().get("schemaValidationMode")).isEqualTo(mode);
  }
}
