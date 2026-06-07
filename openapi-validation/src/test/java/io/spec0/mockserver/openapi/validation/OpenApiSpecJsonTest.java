package io.spec0.mockserver.openapi.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

class OpenApiSpecJsonTest {

  /** SnakeYAML 2.x default per-document code-point limit that this fix works around. */
  private static final int SNAKEYAML_DEFAULT_LIMIT = 3 * 1024 * 1024;

  /**
   * Builds a valid OpenAPI 3.0 YAML document larger than {@code minCodePoints} by generating many
   * paths/schemas in a loop.
   */
  private static String largeValidYamlSpec(int minCodePoints) {
    StringBuilder sb = new StringBuilder(minCodePoints + 4096);
    sb.append("openapi: 3.0.0\n")
        .append("info:\n")
        .append("  title: Large Spec\n")
        .append("  version: '1.0.0'\n")
        .append("paths:\n");
    int i = 0;
    while (sb.length() < minCodePoints) {
      sb.append("  /resource").append(i).append(":\n");
      sb.append("    get:\n");
      sb.append("      operationId: getResource").append(i).append("\n");
      sb.append("      summary: Padding to inflate the document size for resource ").append(i);
      sb.append(" so that the overall spec comfortably exceeds SnakeYAML's default limit\n");
      sb.append("      responses:\n");
      sb.append("        '200':\n");
      sb.append("          description: ok\n");
      sb.append("          content:\n");
      sb.append("            application/json:\n");
      sb.append("              schema:\n");
      sb.append("                type: object\n");
      sb.append("                properties:\n");
      sb.append("                  id:\n");
      sb.append("                    type: integer\n");
      sb.append("                  name:\n");
      sb.append("                    type: string\n");
      sb.append("                  description:\n");
      sb.append("                    type: string\n");
      i++;
    }
    return sb.toString();
  }

  @Test
  void toParseableJson_passesThroughJsonUnchanged() {
    String json = "{\"openapi\":\"3.0.0\"}";
    assertSame(json, OpenApiSpecJson.toParseableJson(json));

    String jsonArray = "[1, 2, 3]";
    assertSame(jsonArray, OpenApiSpecJson.toParseableJson(jsonArray));

    String jsonWithLeadingWhitespace = "  \n  {\"a\":1}";
    assertSame(
        jsonWithLeadingWhitespace, OpenApiSpecJson.toParseableJson(jsonWithLeadingWhitespace));
  }

  @Test
  void toParseableJson_convertsYamlAndKeepsUnquotedScalarsAsText() {
    String yaml =
        """
        openapi: 3.0.0
        info:
          title: T
          version: '1.0'
          x-released: 2020-01-01
        paths: {}
        """;
    String json = OpenApiSpecJson.toParseableJson(yaml);
    assertTrue(json.trim().startsWith("{"), "YAML should be converted to JSON");
    // Date-like scalar must remain a quoted string, not be coerced into a different type.
    assertTrue(json.contains("\"2020-01-01\""), "unquoted date should stay text: " + json);
  }

  @Test
  void rawLargeYaml_breaksWithoutTheFix() {
    // Sanity check: a >3 MB YAML document does trip SnakeYAML's default limit when handed straight
    // to swagger-parser. This is the failure the fix addresses.
    String bigYaml = largeValidYamlSpec(SNAKEYAML_DEFAULT_LIMIT + 512 * 1024);
    assertTrue(
        bigYaml.codePointCount(0, bigYaml.length()) > SNAKEYAML_DEFAULT_LIMIT,
        "test spec must exceed SnakeYAML's default limit");

    ParseOptions opts = new ParseOptions();
    opts.setResolve(false);
    SwaggerParseResult raw = new OpenAPIV3Parser().readContents(bigYaml, null, opts);
    assertNull(
        raw.getOpenAPI(),
        "raw large YAML should fail to parse via swagger-parser due to SnakeYAML's 3 MB limit");
  }

  @Test
  void largeYamlSpec_isParseableAfterConversion() {
    // ~4 MB valid OpenAPI YAML document.
    String bigYaml = largeValidYamlSpec(4 * 1024 * 1024);
    assertTrue(bigYaml.codePointCount(0, bigYaml.length()) > SNAKEYAML_DEFAULT_LIMIT);

    String json = OpenApiSpecJson.toParseableJson(bigYaml);
    assertTrue(json.trim().startsWith("{"), "large YAML should be converted to JSON");

    ParseOptions opts = new ParseOptions();
    opts.setResolve(false);
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(json, null, opts);
    OpenAPI api = result.getOpenAPI();
    assertNotNull(api, "converted large spec should parse: " + result.getMessages());
    assertNotNull(api.getPaths());
    assertFalse(api.getPaths().isEmpty(), "parsed spec should retain its paths");
  }
}
