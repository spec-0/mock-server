package io.spec0.mockserver.openapi.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates JSON instances against OpenAPI {@link Schema} fragments using networknt
 * json-schema-validator <strong>2.x</strong> ({@link SchemaRegistry}, {@link Schema}, {@link
 * Error}). See
 * <a href="https://github.com/networknt/json-schema-validator/blob/master/doc/migration-2.0.0.md">migration-2.0.0</a>.
 *
 * <p>OpenAPI 3.0 vs 3.1 use different JSON Schema dialects; we select {@link
 * Dialects#getOpenApi30()} or {@link Dialects#getOpenApi31()} from the parent document's {@code
 * openapi} field.
 */
public final class DefaultMockOpenApiValidator implements MockOpenApiValidator {

  private static final Logger log = LoggerFactory.getLogger(DefaultMockOpenApiValidator.class);

  private static final SchemaRegistry REGISTRY_OPENAPI_30 =
      SchemaRegistry.withDialect(Dialects.getOpenApi30());

  private static final SchemaRegistry REGISTRY_OPENAPI_31 =
      SchemaRegistry.withDialect(Dialects.getOpenApi31());

  private final ParsedOpenApiCache cache;
  private final ObjectMapper mapper;

  public DefaultMockOpenApiValidator(ParsedOpenApiCache cache, ObjectMapper mapper) {
    this.cache = Objects.requireNonNull(cache);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public OpenApiValidationResult validateRequestBody(
      UUID specId, String operationId, JsonNode body, boolean bodyPresent) {
    if (log.isTraceEnabled()) {
      log.trace(
          "validateRequestBody specId={} operationId={} bodyPresent={} dataSummary={}",
          specId,
          operationId,
          bodyPresent,
          jsonDataSummary(body));
    }
    try {
      OpenAPI api = cache.get(specId);
      Operation op = findOperation(api, operationId);
      if (op == null) {
        traceSkip("request", specId, operationId, null, "operation_not_found_in_openapi");
        return OpenApiValidationResult.skipped("operation_not_found_in_openapi");
      }
      RequestBody rb = op.getRequestBody();
      if (rb == null) {
        traceSkip("request", specId, operationId, null, "no_request_body");
        return OpenApiValidationResult.skipped("no_request_body");
      }
      MediaType json = jsonMediaType(rb.getContent());
      if (json == null || json.getSchema() == null) {
        traceSkip("request", specId, operationId, null, "no_json_request_schema");
        return OpenApiValidationResult.skipped("no_json_request_schema");
      }
      boolean required = Boolean.TRUE.equals(rb.getRequired());
      if (!bodyPresent) {
        if (required) {
          log.trace(
              "validateRequestBody specId={} operationId={} outcome=failure reason=required_body_missing",
              specId,
              operationId);
          return OpenApiValidationResult.failure(List.of("Request body is required"));
        }
        traceSkip("request", specId, operationId, null, "optional_request_body_omitted");
        return OpenApiValidationResult.skipped("optional_request_body_omitted");
      }
      return validateInstance("request", specId, operationId, null, api, json.getSchema(), body);
    } catch (IllegalArgumentException e) {
      log.trace(
          "validateRequestBody specId={} operationId={} outcome=exception type=IllegalArgument msg={}",
          specId,
          operationId,
          e.getMessage());
      return OpenApiValidationResult.failure(List.of(e.getMessage()));
    } catch (RuntimeException e) {
      log.trace(
          "validateRequestBody specId={} operationId={} outcome=exception type={} msg={}",
          specId,
          operationId,
          e.getClass().getSimpleName(),
          e.getMessage());
      return OpenApiValidationResult.failure(
          List.of("request_validation_error: " + e.getMessage()));
    }
  }

  @Override
  public OpenApiValidationResult validateResponseBody(
      UUID specId, String operationId, String statusCode, JsonNode body, boolean bodyPresent) {
    if (log.isTraceEnabled()) {
      log.trace(
          "validateResponseBody specId={} operationId={} statusCode={} bodyPresent={} dataSummary={}",
          specId,
          operationId,
          statusCode,
          bodyPresent,
          jsonDataSummary(body));
    }
    try {
      OpenAPI api = cache.get(specId);
      Operation op = findOperation(api, operationId);
      if (op == null) {
        traceSkip("response", specId, operationId, statusCode, "operation_not_found_in_openapi");
        return OpenApiValidationResult.skipped("operation_not_found_in_openapi");
      }
      if (op.getResponses() == null) {
        traceSkip("response", specId, operationId, statusCode, "no_responses");
        return OpenApiValidationResult.skipped("no_responses");
      }
      String statusKey = statusCode == null ? "" : statusCode.trim();
      ApiResponse resp = op.getResponses().get(statusKey);
      if (resp == null) {
        resp = op.getResponses().get("default");
      }
      if (resp == null) {
        traceSkip(
            "response",
            specId,
            operationId,
            statusCode,
            "no_response_for_status (tried status="
                + statusKey
                + " and default)");
        return OpenApiValidationResult.skipped("no_response_for_status");
      }
      MediaType json = jsonMediaType(resp.getContent());
      if (json == null || json.getSchema() == null) {
        traceSkip("response", specId, operationId, statusCode, "no_json_response_schema");
        return OpenApiValidationResult.skipped("no_json_response_schema");
      }
      if (!bodyPresent) {
        return validateInstance(
            "response", specId, operationId, statusCode, api, json.getSchema(), mapper.nullNode());
      }
      return validateInstance(
          "response", specId, operationId, statusCode, api, json.getSchema(), body);
    } catch (IllegalArgumentException e) {
      log.trace(
          "validateResponseBody specId={} operationId={} statusCode={} outcome=exception type=IllegalArgument msg={}",
          specId,
          operationId,
          statusCode,
          e.getMessage());
      return OpenApiValidationResult.failure(List.of(e.getMessage()));
    } catch (RuntimeException e) {
      log.trace(
          "validateResponseBody specId={} operationId={} statusCode={} outcome=exception type={} msg={}",
          specId,
          operationId,
          statusCode,
          e.getClass().getSimpleName(),
          e.getMessage());
      return OpenApiValidationResult.failure(
          List.of("response_validation_error: " + e.getMessage()));
    }
  }

  private MediaType jsonMediaType(io.swagger.v3.oas.models.media.Content content) {
    if (content == null) {
      return null;
    }
    MediaType mt = content.get("application/json");
    if (mt != null) {
      return mt;
    }
    return content.get("application/*+json");
  }

  private Operation findOperation(OpenAPI api, String operationId) {
    if (api.getPaths() == null) {
      return null;
    }
    for (var pathItem : api.getPaths().values()) {
      if (pathItem.readOperationsMap() == null) {
        continue;
      }
      for (Operation op : pathItem.readOperationsMap().values()) {
        if (op != null && operationId.equals(op.getOperationId())) {
          return op;
        }
      }
    }
    return null;
  }

  private OpenApiValidationResult validateInstance(
      String phase,
      UUID specId,
      String operationId,
      String statusCode,
      OpenAPI api,
      Schema schema,
      JsonNode data) {
    JsonNode schemaNode = Json.mapper().convertValue(schema, JsonNode.class);
    if (schemaNode == null || schemaNode.isNull()) {
      traceSkip(phase, specId, operationId, statusCode, "schema_not_serializable");
      return OpenApiValidationResult.skipped("schema_not_serializable");
    }
    String oaiVer = api.getOpenapi();
    SchemaRegistry registry = pickRegistry(api);
    if (log.isTraceEnabled()) {
      log.trace(
          "validateInstance phase={} specId={} operationId={} statusCode={} openApiVersion={} dialect={} schemaSummary={} dataSummary={}",
          phase,
          specId,
          operationId,
          statusCode,
          oaiVer,
          registry == REGISTRY_OPENAPI_31 ? "openapi-3.1" : "openapi-3.0",
          jsonSchemaSummary(schemaNode),
          jsonDataSummary(data));
    }
    com.networknt.schema.Schema compiled = registry.getSchema(schemaNode);
    compiled.initializeValidators();
    List<Error> raw = compiled.validate(data);
    if (raw.isEmpty()) {
      log.trace(
          "validateInstance phase={} specId={} operationId={} statusCode={} outcome=ok",
          phase,
          specId,
          operationId,
          statusCode);
      return OpenApiValidationResult.ok();
    }
    List<String> errors = new ArrayList<>();
    for (Error e : raw) {
      errors.add(e.toString());
    }
    errors.sort(String::compareTo);
    if (log.isTraceEnabled()) {
      log.trace(
          "validateInstance phase={} specId={} operationId={} statusCode={} outcome=failure errorCount={} errors={}",
          phase,
          specId,
          operationId,
          statusCode,
          errors.size(),
          errors);
    }
    return OpenApiValidationResult.failure(errors);
  }

  private void traceSkip(
      String phase, UUID specId, String operationId, String statusCode, String reason) {
    if (log.isTraceEnabled()) {
      log.trace(
          "openApiValidation phase={} specId={} operationId={} statusCode={} outcome=skipped reason={}",
          phase,
          specId,
          operationId,
          statusCode,
          reason);
    }
  }

  private static String jsonDataSummary(JsonNode data) {
    if (data == null || data.isNull() || data.isMissingNode()) {
      return "null";
    }
    if (data.isObject()) {
      return "object(size=" + data.size() + ")";
    }
    if (data.isArray()) {
      return "array(len=" + data.size() + ")";
    }
    return data.getNodeType().toString();
  }

  private static String jsonSchemaSummary(JsonNode schemaNode) {
    if (schemaNode == null || schemaNode.isNull()) {
      return "null";
    }
    if (schemaNode.hasNonNull("$ref")) {
      return "ref(" + schemaNode.get("$ref").asText() + ")";
    }
    List<String> keys = new ArrayList<>();
    schemaNode.fieldNames().forEachRemaining(k -> keys.add(k));
    keys.sort(String::compareTo);
    int n = keys.size();
    if (n <= 8) {
      return "keys=" + keys;
    }
    return "keys(count=" + n + ", first=" + keys.subList(0, 5) + "...)";
  }

  private static SchemaRegistry pickRegistry(OpenAPI api) {
    String ver = api.getOpenapi();
    if (ver != null && ver.trim().startsWith("3.1")) {
      return REGISTRY_OPENAPI_31;
    }
    return REGISTRY_OPENAPI_30;
  }
}
