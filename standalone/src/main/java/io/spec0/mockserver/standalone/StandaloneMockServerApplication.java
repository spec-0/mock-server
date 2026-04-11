package io.spec0.mockserver.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Self-hosted standalone mock server.
 *
 * <p>Provides the full mock server management REST API and UI backed by an embedded H2 database. No
 * platform authentication, teams, or org management — this is the open-source core.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>Mock requests: {@code ANY /mock/{mockServerId}/**}
 *   <li>Spec management: {@code /mock-server/specs}
 *   <li>Server management: {@code /mock-server/servers}
 *   <li>Variant management: {@code /mock-server/servers/{id}/variants}
 *   <li>Request logs: {@code /mock-server/servers/{id}/logs}
 *   <li>UI: {@code /ui} (served from static resources)
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "io.spec0.mockserver")
public class StandaloneMockServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(StandaloneMockServerApplication.class, args);
  }
}
