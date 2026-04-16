package io.spec0.mockserver.engine.dispatch;

import static org.junit.jupiter.api.Assertions.*;

import io.spec0.mockserver.engine.model.MockServerOperation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationMatcherTest {

  @Test
  void resolvesExactPath() {
    UUID sid = UUID.randomUUID();
    List<MockServerOperation> ops =
        List.of(new MockServerOperation(sid, "listX", "GET", "/x", "200"));
    var r = OperationMatcher.resolve(ops, "/x", "GET", null);
    assertEquals("listX", r.operationId());
  }

  @Test
  void resolvesTemplatePath() {
    UUID sid = UUID.randomUUID();
    List<MockServerOperation> ops =
        List.of(new MockServerOperation(sid, "getOne", "GET", "/x/{id}", "200"));
    var r = OperationMatcher.resolve(ops, "/x/42", "GET", null);
    assertEquals("getOne", r.operationId());
    assertEquals("42", r.pathParams().get("id"));
  }
}
