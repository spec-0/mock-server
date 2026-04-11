package io.spec0.mockserver.standalone;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configures the embedded Next.js UI as a Single-Page Application (SPA).
 *
 * <p>The Next.js static export generates a single {@code index.html} that handles all client-side
 * routes. Spring Boot's default resource handler serves exact file matches only, so a direct
 * browser access to {@code /ui/some-uuid} (detail page) or a hard refresh would return 404.
 *
 * <p>This config registers a custom {@link PathResourceResolver} for {@code /ui/**} that falls back
 * to {@code index.html} when no exact static file is found — the standard SPA pattern. Actual
 * static assets ({@code _next/static/…}, CSS, JS) are still served directly.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/ui/**")
        .addResourceLocations("classpath:/static/ui/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource resource = super.getResource(resourcePath, location);
                if (resource != null && resource.exists() && resource.isReadable()) {
                  return resource;
                }
                // SPA fallback: return index.html for any unmatched path (Next.js handles routing)
                Resource index = location.createRelative("index.html");
                return index.exists() ? index : null;
              }
            });
  }
}
