package io.spec0.mockserver.openapi.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Validates JSON request/response bodies using {@code application/json} schemas from a resolved
 * OpenAPI {@code Operation}.
 */
public interface MockOpenApiValidator {

  OpenApiValidationResult validateRequestBody(
      UUID specId, String operationId, JsonNode body, boolean bodyPresent);

  OpenApiValidationResult validateResponseBody(
      UUID specId, String operationId, String statusCode, JsonNode body, boolean bodyPresent);
}
