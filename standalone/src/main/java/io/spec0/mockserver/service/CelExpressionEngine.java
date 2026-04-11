package io.spec0.mockserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOptions;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelFunctionOverload;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sandboxed CEL (Common Expression Language) expression engine for dynamic mock responses.
 *
 * <p>CEL is Turing-incomplete and cannot perform file I/O or network calls, making it safe to
 * evaluate arbitrary user expressions server-side.
 *
 * <p>Expressions receive two context variables:
 *
 * <ul>
 *   <li>{@code request} — map with method, path, path_params, query_params, headers, body
 *   <li>{@code env} — per-server environment variables (key → value strings)
 * </ul>
 *
 * <p><strong>CEL map literals:</strong> use single-quoted keys (e.g. {@code {'status': 200}}). A
 * bare identifier as a map key is a variable reference, not a string key. Request header names are
 * lowercased (e.g. {@code Session-Id} → {@code session-id}).
 *
 * <p>Expressions must evaluate to a map with:
 *
 * <ul>
 *   <li>{@code status} (int) — HTTP status code
 *   <li>{@code body} (any) — response body (optional)
 *   <li>{@code headers} (map&lt;string,string&gt;) — extra response headers (optional)
 * </ul>
 *
 * <p>Built-in custom functions: {@code uuid()}, {@code now()}, {@code randomInt(min, max)}.
 */
@Service
@Slf4j
public class CelExpressionEngine {

  private final CelCompiler compiler;
  private final CelRuntime runtime;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public CelExpressionEngine() {
    this.compiler =
        CelCompilerFactory.standardCelCompilerBuilder()
            .setOptions(CelOptions.current().build())
            .addVar("request", SimpleType.DYN)
            .addVar("env", SimpleType.DYN)
            .addFunctionDeclarations(
                CelFunctionDecl.newFunctionDeclaration(
                    "uuid", CelOverloadDecl.newGlobalOverload("uuid_0", SimpleType.STRING)),
                CelFunctionDecl.newFunctionDeclaration(
                    "now", CelOverloadDecl.newGlobalOverload("now_0", SimpleType.STRING)),
                CelFunctionDecl.newFunctionDeclaration(
                    "randomInt",
                    CelOverloadDecl.newGlobalOverload(
                        "random_int_2", SimpleType.INT, SimpleType.INT, SimpleType.INT)))
            .build();

    this.runtime =
        CelRuntimeFactory.standardCelRuntimeBuilder()
            .addFunctionBindings(
                // 0-arg functions: use the raw CelFunctionOverload form with empty arg list
                CelFunctionBinding.from(
                    "uuid_0",
                    ImmutableList.of(),
                    (CelFunctionOverload) args -> UUID.randomUUID().toString()),
                CelFunctionBinding.from(
                    "now_0",
                    ImmutableList.of(),
                    (CelFunctionOverload) args -> Instant.now().toString()),
                // 2-arg function: Long params (CEL integers are 64-bit)
                CelFunctionBinding.from(
                    "random_int_2",
                    Long.class,
                    Long.class,
                    (a, b) -> ThreadLocalRandom.current().nextLong(a, b)))
            .build();
  }

  public record CelResult(int status, JsonNode body, Map<String, String> headers) {}

  public record CelRequestContext(
      String method,
      String path,
      Map<String, Object> pathParams,
      Map<String, Object> queryParams,
      Map<String, Object> headers,
      JsonNode body) {}

  /**
   * Evaluates a CEL expression against the request context and server env vars.
   *
   * @return result if the expression evaluates successfully, empty if the expression throws or
   *     returns a non-map value (caller falls back to static responseBody)
   */
  public Optional<CelResult> evaluate(
      String expression, CelRequestContext ctx, Map<String, String> envVars) {

    try {
      CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
      CelRuntime.Program program = runtime.createProgram(ast);

      Map<String, Object> requestMap = buildRequestMap(ctx);
      Map<String, Object> activation = new HashMap<>();
      activation.put("request", requestMap);
      activation.put("env", new HashMap<>(envVars));

      Object result = program.eval(activation);
      return extractResult(result);

    } catch (CelEvaluationException e) {
      log.warn("CEL evaluation error: {}", e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      log.warn("CEL compile/eval error: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private Map<String, Object> buildRequestMap(CelRequestContext ctx) {
    Map<String, Object> map = new HashMap<>();
    map.put("method", ctx.method());
    map.put("path", ctx.path());
    map.put("path_params", new HashMap<>(ctx.pathParams()));
    map.put("query_params", new HashMap<>(ctx.queryParams()));
    map.put("headers", new HashMap<>(ctx.headers()));
    if (ctx.body() != null && !ctx.body().isNull()) {
      map.put("body", objectMapper.convertValue(ctx.body(), Object.class));
    } else {
      map.put("body", null);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private Optional<CelResult> extractResult(Object raw) {
    if (!(raw instanceof Map<?, ?> resultMap)) {
      log.warn(
          "CEL expression did not return a map, got: {}", raw == null ? "null" : raw.getClass());
      return Optional.empty();
    }

    Object statusRaw = resultMap.get("status");
    if (statusRaw == null) {
      log.warn("CEL result map missing required 'status' key");
      return Optional.empty();
    }

    int status;
    try {
      status = ((Number) statusRaw).intValue();
    } catch (ClassCastException e) {
      log.warn("CEL result 'status' is not a number: {}", statusRaw);
      return Optional.empty();
    }

    JsonNode body = null;
    if (resultMap.containsKey("body")) {
      body = objectMapper.valueToTree(resultMap.get("body"));
    }

    Map<String, String> headers = new HashMap<>();
    Object headersRaw = resultMap.get("headers");
    if (headersRaw instanceof Map<?, ?> headersMap) {
      headersMap.forEach(
          (k, v) -> {
            if (k instanceof String && v instanceof String) {
              headers.put((String) k, (String) v);
            }
          });
    }

    return Optional.of(new CelResult(status, body, headers));
  }
}
