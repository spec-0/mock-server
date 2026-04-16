package io.spec0.mockserver.engine.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Framework-agnostic representation of an inbound mock request. Adapters (standalone Spring,
 * platform Spring, Quarkus) convert their native request type into this record before calling
 * {@link MockRequestDispatcher#dispatch}.
 */
public record MockRequest(
    String method,
    String path,
    Map<String, String> queryParams,
    Map<String, String> headers,
    JsonNode body,
    byte[] rawBodyBytes,
    String operationIdOverride,
    String preferredStatusCode) {}
