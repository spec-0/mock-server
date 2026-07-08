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
 * Drives the full HTTP stack to verify that STRICT schema validation rejects requests missing
 * required <em>parameters</em> (query / header), and that WARN/OFF still serve the mock. This
 * exercises the {@code /mock/{id}} dispatch path (not variant-save validation).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:paramvalidtest;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
    })
class RequestParameterValidationIntegrationTest {

  @Autowired private TestRestTemplate rest;

  private static final String SPEC_WITH_REQUIRED_PARAMS =
      """
      openapi: 3.0.0
      info:
        title: Param Validation API
        version: 1.0.0
      paths:
        /search:
          get:
            operationId: search
            parameters:
              - name: q
                in: query
                required: true
                schema:
                  type: string
              - name: X-Tenant-Id
                in: header
                required: true
                schema:
                  type: string
            responses:
              "200":
                description: ok
                content:
                  application/json:
                    schema:
                      type: object
                      properties:
                        result:
                          type: string
      """;

  private String mockServerId;

  private HttpHeaders json() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  @BeforeEach
  void setUp() {
    ResponseEntity<Map> specResp =
        rest.postForEntity(
            "/mock-server/specs",
            new HttpEntity<>(
                Map.of("specName", "param-validation", "specContent", SPEC_WITH_REQUIRED_PARAMS),
                json()),
            Map.class);
    assertThat(specResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String specId = (String) specResp.getBody().get("specId");

    ResponseEntity<Map> serverResp =
        rest.postForEntity(
            "/mock-server/servers",
            new HttpEntity<>(
                Map.of("specId", specId, "name", "param-server", "defaultStrategy", "DEFAULT_ONLY"),
                json()),
            Map.class);
    assertThat(serverResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    mockServerId = (String) serverResp.getBody().get("mockServerId");
  }

  private void setValidationMode(String mode) {
    ResponseEntity<Map> patchResp =
        rest.exchange(
            "/mock-server/servers/" + mockServerId + "/config",
            HttpMethod.PATCH,
            new HttpEntity<>(Map.of("schemaValidationMode", mode), json()),
            Map.class);
    assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void strict_missingRequiredQueryAndHeader_rejectedWith400() {
    setValidationMode("STRICT");

    ResponseEntity<Map> resp = rest.getForEntity("/mock/" + mockServerId + "/search", Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resp.getBody().get("error")).isEqualTo("request_validation_failed");
    @SuppressWarnings("unchecked")
    List<String> details = (List<String>) resp.getBody().get("details");
    assertThat(details).isNotNull();
    assertThat(details).anyMatch(d -> d.contains("query parameter 'q'"));
    assertThat(details).anyMatch(d -> d.contains("header 'X-Tenant-Id'"));
  }

  @Test
  void strict_allRequiredParamsPresent_servesMock() {
    setValidationMode("STRICT");

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Tenant-Id", "acme");
    ResponseEntity<String> resp =
        rest.exchange(
            "/mock/" + mockServerId + "/search?q=widgets",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getHeaders().getFirst("X-spec0-Mock-Response")).isEqualTo("true");
  }

  @Test
  void warn_missingRequiredParams_stillServesMock() {
    setValidationMode("WARN");

    ResponseEntity<String> resp =
        rest.getForEntity("/mock/" + mockServerId + "/search", String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getHeaders().getFirst("X-spec0-Mock-Response")).isEqualTo("true");
  }

  @Test
  void off_missingRequiredParams_servesMock() {
    // Default mode is OFF — no validation.
    ResponseEntity<String> resp =
        rest.getForEntity("/mock/" + mockServerId + "/search", String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getHeaders().getFirst("X-spec0-Mock-Response")).isEqualTo("true");
  }
}
