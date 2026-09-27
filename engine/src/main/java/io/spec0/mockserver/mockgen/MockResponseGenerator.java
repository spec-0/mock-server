package io.spec0.mockserver.mockgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a sample response body from an OpenAPI response or schema.
 *
 * <p>Data written in the spec always wins over generated data. For each value the generator uses
 * the first of these that is present:
 *
 * <ol>
 *   <li>the response media type's {@code example}, or the first entry of its {@code examples}
 *   <li>the schema's {@code example} (also on {@code $ref}'d component schemas, array {@code items}
 *       and each property)
 *   <li>the first element of the schema's {@code examples} array (OpenAPI 3.1)
 *   <li>the schema's {@code default}
 *   <li>the schema's {@code const}
 *   <li>the first value of the schema's {@code enum}
 *   <li>a generated value that follows the schema's {@code format} (email, uuid, date-time, ...)
 *   <li>a random placeholder value
 * </ol>
 *
 * <p>Objects and arrays are built field by field with the same rules, so a property example is used
 * even when the enclosing object has none. {@code allOf} parts are merged; for {@code oneOf} /
 * {@code anyOf} the first option is used.
 */
public class MockResponseGenerator {
  /** Guards against deeply nested or self-referencing schemas. */
  private static final int MAX_DEPTH = 32;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Faker faker = new Faker();
  private final JsonNode root; // Whole OpenAPI document, used to resolve local $refs

  public MockResponseGenerator(JsonNode rootNode) {
    this.root = rootNode;
  }

  public MockResponseGenerator(String openApiSpecString) throws Exception {
    this(MAPPER.readTree(openApiSpecString));
  }

  /**
   * Generates sample data for either an OpenAPI response object (with {@code content}) or a schema.
   */
  public Object generateMockData(JsonNode schema) {
    return generate(schema, new ArrayDeque<>(), 0);
  }

  private Object generate(JsonNode node, Deque<String> refStack, int depth) {
    if (node == null || !node.isObject() || depth > MAX_DEPTH) return null;

    if (node.has("$ref")) {
      // OpenAPI 3.1 allows siblings next to $ref; an example written there takes priority.
      if (hasExample(node)) return exampleOf(node);
      String ref = node.get("$ref").asText();
      if (refStack.contains(ref)) return null; // self-referencing schema: stop here
      JsonNode target = resolveRef(ref);
      if (target == null) return null;
      refStack.push(ref);
      try {
        return generate(target, refStack, depth + 1);
      } finally {
        refStack.pop();
      }
    }

    // Response object: pick a media type, prefer its examples, then its schema.
    if (node.has("content")) {
      JsonNode media = pickMediaType(node.get("content"));
      if (media == null) return null;
      if (media.has("example")) return toJava(media.get("example"));
      Object named = firstNamedExample(media.get("examples"));
      if (named != null) return named;
      return generate(media.get("schema"), refStack, depth + 1);
    }

    return generateFromSchema(node, refStack, depth);
  }

  private Object generateFromSchema(JsonNode schema, Deque<String> refStack, int depth) {
    if (hasExample(schema)) return exampleOf(schema);
    if (schema.has("default")) return toJava(schema.get("default"));
    if (schema.has("const")) return toJava(schema.get("const"));
    JsonNode enumNode = schema.get("enum");
    if (enumNode != null && enumNode.isArray() && !enumNode.isEmpty()) {
      return toJava(enumNode.get(0));
    }

    if (schema.has("allOf")) return generateAllOf(schema, refStack, depth);
    for (String key : new String[] {"oneOf", "anyOf"}) {
      JsonNode options = schema.get(key);
      if (options != null && options.isArray() && !options.isEmpty()) {
        return generate(options.get(0), refStack, depth + 1);
      }
    }

    String type = typeOf(schema);
    if (type == null) return null;
    return switch (type) {
      case "string" -> generateStringMock(schema);
      case "integer" -> faker.number().numberBetween(1, 1000);
      case "number" -> faker.number().randomDouble(2, 1, 1000);
      case "boolean" -> faker.bool().bool();
      case "array" -> generateArrayMock(schema, refStack, depth);
      case "object" -> generateObjectMock(schema, refStack, depth);
      default -> null;
    };
  }

  /** Merges the objects produced by each {@code allOf} part plus the schema's own properties. */
  private Object generateAllOf(JsonNode schema, Deque<String> refStack, int depth) {
    Map<String, Object> merged = new LinkedHashMap<>();
    Object lastNonObject = null;
    for (JsonNode part : schema.get("allOf")) {
      Object value = generate(part, refStack, depth + 1);
      if (value instanceof Map<?, ?> map) {
        map.forEach((k, v) -> merged.put(String.valueOf(k), v));
      } else if (value != null) {
        lastNonObject = value;
      }
    }
    if (schema.has("properties")) {
      merged.putAll(generateObjectMock(schema, refStack, depth));
    }
    return merged.isEmpty() && lastNonObject != null ? lastNonObject : merged;
  }

  /**
   * Returns the schema type. Handles OpenAPI 3.1 type arrays (e.g. {@code ["string", "null"]}) and
   * infers object/array when {@code type} is left out.
   */
  private static String typeOf(JsonNode schema) {
    JsonNode type = schema.get("type");
    if (type != null && type.isTextual()) return type.asText();
    if (type != null && type.isArray()) {
      for (JsonNode t : type) {
        if (!"null".equals(t.asText())) return t.asText();
      }
      return null;
    }
    if (schema.has("properties") || schema.has("additionalProperties")) return "object";
    if (schema.has("items")) return "array";
    return null;
  }

  private String generateStringMock(JsonNode schema) {
    if (schema.has("format")) {
      String format = schema.get("format").asText();
      return switch (format) {
        case "uuid" -> faker.internet().uuid();
        case "email" -> faker.internet().emailAddress();
        case "date-time" -> faker.date().birthday().toInstant().toString();
        case "date" ->
            LocalDate.ofInstant(faker.date().birthday().toInstant(), ZoneOffset.UTC).toString();
        case "uri", "url" -> "https://" + faker.internet().domainName();
        case "hostname" -> faker.internet().domainName();
        case "ipv4" -> faker.internet().ipV4Address();
        case "ipv6" -> faker.internet().ipV6Address();
        default -> faker.lorem().word();
      };
    }
    return faker.lorem().word();
  }

  private List<Object> generateArrayMock(JsonNode schema, Deque<String> refStack, int depth) {
    List<Object> items = new ArrayList<>();
    if (schema.has("items")) {
      items.add(generate(schema.get("items"), refStack, depth + 1));
    }
    return items;
  }

  private Map<String, Object> generateObjectMock(
      JsonNode schema, Deque<String> refStack, int depth) {
    Map<String, Object> mockObject = new LinkedHashMap<>();
    if (schema.has("properties")) {
      schema
          .get("properties")
          .fields()
          .forEachRemaining(
              field ->
                  mockObject.put(field.getKey(), generate(field.getValue(), refStack, depth + 1)));
    }
    return mockObject;
  }

  /** Prefers a JSON media type, otherwise the first one listed. */
  private static JsonNode pickMediaType(JsonNode content) {
    if (content == null || !content.isObject()) return null;
    JsonNode first = null;
    Iterator<Map.Entry<String, JsonNode>> it = content.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = it.next();
      if (first == null) first = entry.getValue();
      String mediaType = entry.getKey().toLowerCase();
      if (mediaType.startsWith("application/json") || mediaType.contains("+json")) {
        return entry.getValue();
      }
    }
    return first;
  }

  /** First usable {@code value} from a media type's named {@code examples} map. */
  private Object firstNamedExample(JsonNode examples) {
    if (examples == null || !examples.isObject()) return null;
    for (JsonNode example : examples) {
      JsonNode resolved = example.has("$ref") ? resolveRef(example.get("$ref").asText()) : example;
      if (resolved != null && resolved.has("value")) return toJava(resolved.get("value"));
    }
    return null;
  }

  private static boolean hasExample(JsonNode schema) {
    if (schema.has("example")) return true;
    JsonNode examples = schema.get("examples");
    return examples != null && examples.isArray() && !examples.isEmpty();
  }

  private static Object exampleOf(JsonNode schema) {
    if (schema.has("example")) return toJava(schema.get("example"));
    return toJava(schema.get("examples").get(0));
  }

  private static Object toJava(JsonNode value) {
    return MAPPER.convertValue(value, Object.class);
  }

  /** Resolves a local reference such as {@code #/components/schemas/Book}. */
  private JsonNode resolveRef(String ref) {
    if (root == null || ref == null || !ref.startsWith("#/")) return null;
    JsonNode target = root.at(ref.substring(1));
    return target.isMissingNode() ? null : target;
  }
}
