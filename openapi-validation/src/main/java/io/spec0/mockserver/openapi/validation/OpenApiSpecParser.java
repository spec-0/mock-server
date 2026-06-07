package io.spec0.mockserver.openapi.validation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parses and fully resolves OpenAPI documents. */
public final class OpenApiSpecParser {

  private static final Logger log = LoggerFactory.getLogger(OpenApiSpecParser.class);

  public OpenAPI parse(String specContent) {
    ParseOptions opts = new ParseOptions();
    opts.setResolve(true);
    // Inline '#/components/schemas/...' (and other internal refs) so each operation schema is a
    // self-contained JSON Schema tree. Otherwise networknt resolves $ref only inside the fragment
    // passed to SchemaRegistry and throws InvalidSchemaRefException for component references.
    opts.setResolveFully(true);
    // Pre-convert YAML to JSON with a raised code-point limit so large specs don't trip
    // SnakeYAML's default 3 MB per-document limit inside swagger-parser.
    SwaggerParseResult result =
        new OpenAPIV3Parser()
            .readContents(OpenApiSpecJson.toParseableJson(specContent), null, opts);
    if (result.getOpenAPI() == null) {
      String msg =
          result.getMessages() == null
              ? "unknown error"
              : result.getMessages().stream().collect(Collectors.joining("; "));
      throw new IllegalArgumentException("OpenAPI parse failed: " + msg);
    }
    OpenAPI api = result.getOpenAPI();
    if (log.isTraceEnabled()) {
      int paths = api.getPaths() == null ? 0 : api.getPaths().size();
      int messages = result.getMessages() == null ? 0 : result.getMessages().size();
      log.trace(
          "openApiParse ok openapiVersion={} paths={} parserMessages={} contentChars={}",
          api.getOpenapi(),
          paths,
          messages,
          specContent == null ? 0 : specContent.length());
    }
    return api;
  }
}
