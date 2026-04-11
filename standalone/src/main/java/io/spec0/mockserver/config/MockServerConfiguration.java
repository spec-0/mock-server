package io.spec0.mockserver.config;

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
}
