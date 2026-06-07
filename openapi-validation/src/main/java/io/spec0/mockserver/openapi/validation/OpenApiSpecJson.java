package io.spec0.mockserver.openapi.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.yaml.snakeyaml.LoaderOptions;

/**
 * Normalises a user-supplied OpenAPI document (YAML or JSON) into a JSON string that swagger-parser
 * can consume without tripping SnakeYAML's per-document size limit.
 *
 * <p>swagger-parser builds its own internal SnakeYAML instance and exposes no knob to raise its
 * limits. SnakeYAML 2.x defaults to a 3 MB ({@code 3 * 1024 * 1024}) per-document code-point limit,
 * so a large OpenAPI spec (e.g. ~6 MB of YAML) fails to parse with {@code "The incoming YAML
 * document exceeds the limit"}. JSON parsing is not subject to that limit, so we pre-parse YAML
 * ourselves with a raised limit and re-serialise to JSON before handing the document to
 * swagger-parser.
 */
public final class OpenApiSpecJson {

  /**
   * Maximum number of YAML code points we allow in a single OpenAPI document. Set well above any
   * realistic spec (32 MiB) while still bounding memory use against pathological input. SnakeYAML's
   * own default is only 3 MiB.
   */
  public static final int MAX_SPEC_CODE_POINTS = 32 * 1024 * 1024;

  // Built once and reused — Jackson mappers are thread-safe for read/write operations.
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private static final YAMLMapper YAML_MAPPER = buildYamlMapper();

  private OpenApiSpecJson() {}

  private static YAMLMapper buildYamlMapper() {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setCodePointLimit(MAX_SPEC_CODE_POINTS);
    return new YAMLMapper(YAMLFactory.builder().loaderOptions(loaderOptions).build());
  }

  /**
   * Returns a JSON representation of {@code spec} suitable for feeding to swagger-parser.
   *
   * <p>If the content already looks like JSON (first non-whitespace char is {@code {} or {@code [})
   * it is returned unchanged — JSON parsing is not subject to SnakeYAML's code-point limit. YAML is
   * parsed with a raised limit and re-serialised to JSON. {@code readTree} is used (rather than
   * SnakeYAML's {@code SafeConstructor}) so unquoted scalars and dates stay as text and the document
   * is not coerced or corrupted.
   *
   * <p>If conversion fails for any reason the original content is returned so the caller's existing
   * parse path (and its own error handling) is unchanged.
   */
  public static String toParseableJson(String spec) {
    if (spec == null) {
      return null;
    }
    String trimmed = spec.stripLeading();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      return spec;
    }
    try {
      return JSON_MAPPER.writeValueAsString(YAML_MAPPER.readTree(spec));
    } catch (Exception e) {
      // Fall back to the raw content; the caller's swagger-parser invocation may still cope, and
      // its own error handling stays in control of the failure mode.
      return spec;
    }
  }
}
