package io.spec0.mockserver.engine.model;

/** Normalized media type for flat variant keys when OpenAPI omits {@code content}. */
public final class ContentTypeConstants {

  public static final String ANY = "*/*";

  private ContentTypeConstants() {}

  public static String normalize(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return ANY;
    }
    return contentType.trim();
  }
}
