package io.spec0.mockserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.engine.dispatch.CelEvaluator;
import io.spec0.mockserver.engine.dispatch.MockRequestDispatcher;
import io.spec0.mockserver.engine.service.DefaultApiSpecRegistrationService;
import io.spec0.mockserver.engine.spi.ApiSpecCatalogPersistencePort;
import io.spec0.mockserver.engine.spi.MockServerEnvVarPort;
import io.spec0.mockserver.engine.spi.MockServerPersistencePort;
import io.spec0.mockserver.openapi.validation.MockOpenApiValidator;
import io.spec0.mockserver.repository.MockServerEnvVarRepository;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring wiring for the mock server persistence schema and shared infrastructure. Controllers and
 * domain services are registered via {@code @ComponentScan} on the standalone application.
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.spec0.mockserver.repository")
@EntityScan(basePackages = "io.spec0.mockserver.domain")
public class MockServerConfiguration {

  /**
   * Named Flyway bean that manages the {@code mock_server} schema independently of the host
   * application's default Flyway configuration. Standalone disables {@code spring.flyway} and
   * relies solely on this bean.
   */
  @Bean(name = "mockServerFlyway", initMethod = "migrate")
  @ConditionalOnMissingBean(name = "mockServerFlyway")
  public Flyway mockServerFlyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas("mock_server")
        .createSchemas(true)
        .locations("classpath:db/mock-server")
        .table("flyway_mock_server_history")
        .load();
  }

  @Bean
  @ConditionalOnMissingBean
  public DefaultApiSpecRegistrationService apiSpecRegistrationService(
      ApiSpecCatalogPersistencePort catalog) {
    return new DefaultApiSpecRegistrationService(catalog);
  }

  @Bean
  @ConditionalOnMissingBean
  public MockServerEnvVarPort mockServerEnvVarPort(MockServerEnvVarRepository repo) {
    return id ->
        repo.findByMockServerId(id).stream()
            .collect(Collectors.toMap(e -> e.getVarKey(), e -> e.getVarValue()));
  }

  @Bean
  @ConditionalOnMissingBean
  public MockRequestDispatcher mockRequestDispatcher(
      MockServerPersistencePort persistence,
      MockServerEnvVarPort envVarPort,
      MockOpenApiValidator validator,
      ObjectMapper objectMapper) {
    return new MockRequestDispatcher(
        persistence, envVarPort, new CelEvaluator(), validator, objectMapper);
  }
}
