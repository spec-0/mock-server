package io.spec0.mockserver.openapi.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;

/**
 * Validates JSON request/response bodies using {@code application/json} schemas from a resolved
 * OpenAPI {@code Operation}, and checks that required request parameters are present.
 */
public interface MockOpenApiValidator {

  OpenApiValidationResult validateRequestBody(
      UUID specId, String operationId, JsonNode body, boolean bodyPresent);

  OpenApiValidationResult validateResponseBody(
      UUID specId, String operationId, String statusCode, JsonNode body, boolean bodyPresent);

  /**
   * Checks that every parameter declared {@code required: true} on the operation (or its containing
   * path item) is present in the incoming request. Presence-only: values are not type/format
   * checked. Covers {@code query}, {@code path}, and {@code header} parameters; {@code cookie}
   * parameters are out of scope.
   *
   * <p>Default implementation returns {@link OpenApiValidationResult#skipped(String)} so external
   * embedders implementing this interface are not broken by the added method.
   *
   * @param queryParams query parameters keyed by name
   * @param pathParams resolved path-template variables keyed by name
   * @param headers request headers keyed by <strong>lower-cased</strong> name
   */
  default OpenApiValidationResult validateRequestParameters(
      UUID specId,
      String operationId,
      Map<String, String> queryParams,
      Map<String, String> pathParams,
      Map<String, String> headers) {
    return OpenApiValidationResult.skipped("parameter_validation_unsupported");
  }
}
