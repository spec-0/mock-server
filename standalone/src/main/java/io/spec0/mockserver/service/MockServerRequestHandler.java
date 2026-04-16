package io.spec0.mockserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.engine.dispatch.MockRequest;
import io.spec0.mockserver.engine.dispatch.MockRequestDispatcher;
import io.spec0.mockserver.engine.dispatch.MockResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Thin Spring adapter that converts {@link HttpServletRequest} to {@link MockRequest}, delegates to
 * {@link MockRequestDispatcher}, and converts the result back to {@link ResponseEntity}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockServerRequestHandler {

  private static final String HEADER_OPERATION_ID = "X-Mock-Operation-Id";
  private static final String HEADER_PREFERRED_STATUS = "X-spec0-Preferred-Response-Code";

  private final MockRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;

  public ResponseEntity<JsonNode> handle(UUID mockServerId, HttpServletRequest request) {
    MockRequest mockRequest = adaptRequest(request, mockServerId);
    MockResponse mockResponse = dispatcher.dispatch(mockServerId, mockRequest);
    return adaptResponse(mockResponse);
  }

  // ── Adaptation helpers ───────────────────────────────────────────────────

  private MockRequest adaptRequest(HttpServletRequest request, UUID mockServerId) {
    String path = extractPath(request, mockServerId);
    String method = request.getMethod().toUpperCase();

    Map<String, String> queryParams = new HashMap<>();
    if (request.getQueryString() != null) {
      request.getParameterMap().forEach((k, v) -> queryParams.put(k, v.length > 0 ? v[0] : ""));
    }

    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames())
        .forEach(name -> headers.put(name.toLowerCase(), request.getHeader(name)));

    byte[] rawBodyBytes = readRawBody(request);
    JsonNode body = parseBody(rawBodyBytes);

    return new MockRequest(
        method,
        path,
        queryParams,
        headers,
        body,
        rawBodyBytes,
        request.getHeader(HEADER_OPERATION_ID),
        request.getHeader(HEADER_PREFERRED_STATUS));
  }

  private ResponseEntity<JsonNode> adaptResponse(MockResponse resp) {
    ResponseEntity.BodyBuilder builder =
        ResponseEntity.status(resp.statusCode()).contentType(MediaType.APPLICATION_JSON);
    resp.headers().forEach(builder::header);
    return builder.body(resp.body());
  }

  // ── Utilities ────────────────────────────────────────────────────────────

  private String extractPath(HttpServletRequest request, UUID mockServerId) {
    String uri = request.getRequestURI();
    String prefix = "/mock/" + mockServerId;
    if (uri.contains(prefix)) {
      String path = uri.substring(uri.indexOf(prefix) + prefix.length());
      return path.isEmpty() ? "/" : path;
    }
    return uri;
  }

  private byte[] readRawBody(HttpServletRequest request) {
    if (request instanceof ContentCachingRequestWrapper w) {
      return w.getContentAsByteArray();
    }
    return null;
  }

  private JsonNode parseBody(byte[] raw) {
    if (raw == null || raw.length == 0) return null;
    try {
      return objectMapper.readTree(raw);
    } catch (Exception e) {
      return null;
    }
  }
}
