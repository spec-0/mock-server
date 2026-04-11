package io.spec0.mockserver.openapi.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultMockOpenApiValidatorTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private DefaultMockOpenApiValidator validator30;
  private DefaultMockOpenApiValidator validator31;
  private static final UUID SPEC = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() throws Exception {
    OpenAPI api30 =
        new OpenApiSpecParser()
            .parse(
                new String(
                    getClass().getResourceAsStream("/openapi-30-minimal.yaml").readAllBytes(),
                    StandardCharsets.UTF_8));
    OpenAPI api31 =
        new OpenApiSpecParser()
            .parse(
                new String(
                    getClass().getResourceAsStream("/openapi-31-minimal.yaml").readAllBytes(),
                    StandardCharsets.UTF_8));

    ParsedOpenApiCache cache30 = newFixedCache(api30);
    ParsedOpenApiCache cache31 = newFixedCache(api31);
    validator30 = new DefaultMockOpenApiValidator(cache30, mapper);
    validator31 = new DefaultMockOpenApiValidator(cache31, mapper);
  }

  private static ParsedOpenApiCache newFixedCache(OpenAPI api) {
    return new ParsedOpenApiCache() {
      @Override
      public OpenAPI get(UUID specId) {
        return api;
      }

      @Override
      public void invalidate(UUID specId) {}

      @Override
      public void invalidateAll() {}
    };
  }

  @Test
  void openapi30_requestMatchesSchema() throws Exception {
    JsonNode body = mapper.readTree("{\"name\":\"dog\"}");
    OpenApiValidationResult r =
        validator30.validateRequestBody(SPEC, "createPet", body, true);
    assertTrue(r.valid() || r.skipped(), r.toString());
  }

  @Test
  void openapi30_requestViolatesSchema() throws Exception {
    JsonNode body = mapper.readTree("{\"wrong\":1}");
    OpenApiValidationResult r =
        validator30.validateRequestBody(SPEC, "createPet", body, true);
    assertFalse(r.valid());
  }

  @Test
  void openapi30_responseMatchesSchema() throws Exception {
    JsonNode body = mapper.readTree("{\"id\":\"x\"}");
    OpenApiValidationResult r =
        validator30.validateResponseBody(SPEC, "createPet", "201", body, true);
    assertTrue(r.valid() || r.skipped(), r.toString());
  }

  @Test
  void openapi31_roundTrip() throws Exception {
    JsonNode body = mapper.readTree("{\"x\":1}");
    OpenApiValidationResult r =
        validator31.validateRequestBody(SPEC, "createItem", body, true);
    assertTrue(r.valid() || r.skipped(), r.toString());
  }

  /** Regression: $ref '#/components/schemas/...' must not break networknt (needs resolveFully parse). */
  @Test
  void openapi30_responseWrongDocumentedStatus_isSkippedNotValid() throws Exception {
    JsonNode body = mapper.readTree("{\"anything\":true}");
    OpenApiValidationResult r =
        validator30.validateResponseBody(SPEC, "createPet", "418", body, true);
    assertTrue(r.skipped(), r.toString());
    assertFalse(r.valid(), "skipped must not look like a validation pass");
  }

  /** Bug repro: response body missing a required field must be invalid (not valid, not skipped). */
  @Test
  void openapi30_BUGREPRO_responseMissingRequiredField_isInvalid() throws Exception {
    // 201 response schema for createPet: requires {id: string}. Body {wrong:1} is missing 'id'.
    JsonNode body = mapper.readTree("{\"wrong\":1}");
    OpenApiValidationResult r =
        validator30.validateResponseBody(SPEC, "createPet", "201", body, true);
    assertFalse(r.skipped(), "Must not skip — spec has a 201 application/json schema: " + r);
    assertFalse(r.valid(), "Expected invalid (missing required 'id'), got: " + r);
  }

  /** Bug repro: component-ref schema with missing required field must be invalid (not skipped). */
  @Test
  void openapi30_BUGREPRO_componentRefMissingRequiredField_isInvalid() throws Exception {
    OpenAPI api =
        new OpenApiSpecParser()
            .parse(
                new String(
                    getClass()
                        .getResourceAsStream("/openapi-30-component-ref.yaml")
                        .readAllBytes(),
                    StandardCharsets.UTF_8));
    DefaultMockOpenApiValidator v = new DefaultMockOpenApiValidator(newFixedCache(api), mapper);
    // Cart schema requires {id: string}. Body {wrong:1} is missing required 'id'.
    JsonNode body = mapper.readTree("{\"wrong\":1}");
    OpenApiValidationResult r = v.validateResponseBody(SPEC, "getCart", "200", body, true);
    assertFalse(r.skipped(), "Must not skip — spec has a 200 application/json schema: " + r);
    assertFalse(r.valid(), "Expected invalid (Cart missing required 'id'), got: " + r);
  }

  @Test
  void openapi30_responseUsesComponentSchema_refResolves() throws Exception {
    OpenAPI api =
        new OpenApiSpecParser()
            .parse(
                new String(
                    getClass()
                        .getResourceAsStream("/openapi-30-component-ref.yaml")
                        .readAllBytes(),
                    StandardCharsets.UTF_8));
    DefaultMockOpenApiValidator v = new DefaultMockOpenApiValidator(newFixedCache(api), mapper);
    JsonNode body = mapper.readTree("{\"id\":\"cart-1\"}");
    OpenApiValidationResult r = v.validateResponseBody(SPEC, "getCart", "200", body, true);
    assertTrue(r.valid(), r.toString());
  }
}
