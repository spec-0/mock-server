package io.spec0.mockserver.standalone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Open CORS policy for standalone mode — the UI is served from the same origin, but API calls from
 * the CLI or developer tools may come from any origin.
 */
@Configuration
public class StandaloneCorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.addExposedHeader("X-spec0-Mock-Response");
    config.addExposedHeader("X-spec0-Mock-Variant-Id");
    config.addExposedHeader("X-spec0-Mock-Operation-Id");
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
