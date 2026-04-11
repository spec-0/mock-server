package io.spec0.mockserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.spec0.mockserver.service.MockServerRequestHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Handles all incoming mock API requests. Routes to the appropriate variant via the request handler. */
@RestController
@RequestMapping("/mock/{mockServerId}")
@RequiredArgsConstructor
public class MockRequestController {

  private final MockServerRequestHandler requestHandler;

  @RequestMapping(
      value = {"", "/", "/**"},
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public ResponseEntity<JsonNode> handleMockRequest(
      @PathVariable UUID mockServerId, HttpServletRequest request) {
    return requestHandler.handle(mockServerId, request);
  }

  @RequestMapping(
      value = {"", "/", "/**"},
      method = RequestMethod.OPTIONS)
  public ResponseEntity<Void> handlePreflight() {
    return ResponseEntity.noContent()
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
        // Keep in sync with StandaloneCorsConfig (permissive policy); a narrow list breaks
        // cross-origin tests that send arbitrary custom headers from the UI or devtools.
        .header("Access-Control-Allow-Headers", "*")
        .header(
            "Access-Control-Expose-Headers",
            "X-spec0-Mock-Response, X-spec0-Mock-Variant-Id, X-spec0-Mock-Operation-Id")
        .build();
  }
}
