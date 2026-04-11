package io.spec0.mockserver.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.openapi.validation.CaffeineParsedOpenApiCache;
import io.spec0.mockserver.openapi.validation.DefaultMockOpenApiValidator;
import io.spec0.mockserver.openapi.validation.MockOpenApiValidator;
import io.spec0.mockserver.openapi.validation.OpenApiSpecParser;
import io.spec0.mockserver.openapi.validation.ParsedOpenApiCache;
import io.spec0.mockserver.openapi.validation.SpecContentLoader;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiValidationBeans {

  @Bean
  public OpenApiSpecParser openApiSpecParser() {
    return new OpenApiSpecParser();
  }

  @Bean
  public ParsedOpenApiCache parsedOpenApiCache(
      SpecContentLoader specContentLoader,
      OpenApiSpecParser parser,
      @Value("${mockserver.openapi-cache.ttl-minutes:5}") long ttlMinutes,
      @Value("${mockserver.openapi-cache.max-size:500}") long maxSize) {
    return new CaffeineParsedOpenApiCache(
        specContentLoader, parser, Duration.ofMinutes(ttlMinutes), maxSize);
  }

  @Bean
  public MockOpenApiValidator mockOpenApiValidator(
      ParsedOpenApiCache cache, ObjectMapper objectMapper) {
    return new DefaultMockOpenApiValidator(cache, objectMapper);
  }
}
