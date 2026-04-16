package io.spec0.mockserver.engine.openapi;

import static org.junit.jupiter.api.Assertions.*;

import io.spec0.mockserver.engine.model.MockServerOperation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenApiSpecSupportMinimalTest {

  @Test
  void extractOperations_minimalYamlWithoutOperationId() {
    String spec =
        """
        openapi: 3.0.0
        info:
          title: T
          version: '1.0'
        paths:
          /y:
            get:
              responses:
                '200':
                  description: ok
        """;
    List<MockServerOperation> ops = OpenApiSpecSupport.extractOperations(UUID.randomUUID(), spec);
    assertEquals(1, ops.size());
    assertEquals("GET", ops.get(0).getHttpMethod());
    assertTrue(ops.get(0).getOperationId().startsWith("get_"));
  }

  @Test
  void extractOperations_minimalJsonWithoutOperationId() {
    String spec =
        """
        {
          "openapi": "3.0.0",
          "info": { "title": "T", "version": "1.0.0" },
          "paths": {
            "/x": {
              "get": {
                "responses": { "200": { "description": "ok" } }
              }
            }
          }
        }""";
    List<MockServerOperation> ops = OpenApiSpecSupport.extractOperations(UUID.randomUUID(), spec);
    assertEquals(1, ops.size());
    assertEquals("GET", ops.get(0).getHttpMethod());
    assertEquals("/x", ops.get(0).getPath());
    assertEquals("200", ops.get(0).getSuccessStatusCode());
    assertTrue(ops.get(0).getOperationId().startsWith("get_"));
  }

  @Test
  void sha256_isStable() {
    String a = OpenApiSpecSupport.sha256("hello");
    String b = OpenApiSpecSupport.sha256("hello");
    assertEquals(64, a.length());
    assertEquals(a, b);
  }

  @Test
  void synthesiseOperationId_matchesSlugRule() {
    assertEquals("get__x", OpenApiSpecSupport.synthesiseOperationId("GET", "/x"));
  }
}
