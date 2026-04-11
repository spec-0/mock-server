package io.spec0.mockserver.controller;

import io.spec0.mockserver.domain.MockOperationConfigEntity;
import io.spec0.mockserver.domain.MockResponseStrategy;
import io.spec0.mockserver.domain.MockServerConfigEntity;
import io.spec0.mockserver.domain.MockServerEntity;
import io.spec0.mockserver.domain.MockServerEnvVarEntity;
import io.spec0.mockserver.dto.MockServerExportDto;
import io.spec0.mockserver.dto.MockServerStandaloneConfigDto;
import io.spec0.mockserver.error.MockApiException;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;
import io.spec0.mockserver.port.MockServerServicePort;
import io.spec0.mockserver.repository.MockServerConfigRepository;
import io.spec0.mockserver.repository.MockServerEnvVarRepository;
import io.spec0.mockserver.repository.MockServerOperationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock-server/servers")
@RequiredArgsConstructor
public class MockServerController {

  private final MockServerServicePort mockServerService;
  private final MockServerOperationRepository operationRepository;
  private final MockServerEnvVarRepository envVarRepository;
  private final MockServerConfigRepository configRepository;

  @PostMapping
  public ResponseEntity<MockServerEntity> createMockServer(@RequestBody Map<String, String> body) {
    UUID specId = UUID.fromString(body.get("specId"));
    String name = body.get("name");
    MockResponseStrategy strategy =
        body.containsKey("defaultStrategy")
            ? MockResponseStrategy.valueOf(body.get("defaultStrategy"))
            : MockResponseStrategy.RANDOM;
    MockServerEntity server = mockServerService.createMockServer(specId, name, strategy);
    return ResponseEntity.status(HttpStatus.CREATED).body(server);
  }

  @GetMapping
  public ResponseEntity<List<MockServerEntity>> listMockServers() {
    return ResponseEntity.ok(mockServerService.findAll());
  }

  @GetMapping("/{mockServerId}")
  public ResponseEntity<MockServerEntity> getMockServer(@PathVariable UUID mockServerId) {
    return mockServerService
        .findById(mockServerId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{mockServerId}")
  public ResponseEntity<Void> deleteMockServer(@PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    mockServerService.deleteMockServer(mockServerId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{mockServerId}")
  public ResponseEntity<MockServerEntity> patchMockServer(
      @PathVariable UUID mockServerId, @RequestBody Map<String, Object> body) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    MockServerEntity updated = mockServerService.findById(mockServerId).get();
    if (body.containsKey("name")) {
      updated = mockServerService.updateName(mockServerId, (String) body.get("name"));
    }
    if (body.containsKey("isEnabled")) {
      mockServerService.setEnabled(mockServerId, Boolean.TRUE.equals(body.get("isEnabled")));
      updated = mockServerService.findById(mockServerId).get();
    }
    if (body.containsKey("defaultStrategy")) {
      updated =
          mockServerService.updateStrategy(
              mockServerId, MockResponseStrategy.valueOf((String) body.get("defaultStrategy")));
    }
    return ResponseEntity.ok(updated);
  }

  @GetMapping("/{mockServerId}/operations")
  public ResponseEntity<List<MockOperationConfigEntity>> listOperationConfigs(
      @PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mockServerService.getOperationConfigs(mockServerId));
  }

  @PatchMapping("/{mockServerId}/operations/{operationId}")
  public ResponseEntity<MockOperationConfigEntity> patchOperationConfig(
      @PathVariable UUID mockServerId,
      @PathVariable String operationId,
      @RequestBody Map<String, Object> body) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Boolean enabled = body.containsKey("isEnabled") ? Boolean.TRUE.equals(body.get("isEnabled")) : null;
    MockResponseStrategy strategy = null;
    if (body.containsKey("responseStrategy") && body.get("responseStrategy") != null) {
      strategy = MockResponseStrategy.valueOf((String) body.get("responseStrategy"));
    }
    return ResponseEntity.ok(
        mockServerService.updateOperationConfig(mockServerId, operationId, enabled, strategy));
  }

  /**
   * Returns the list of operations parsed from the spec associated with this mock server.
   * Useful for the UI Endpoints tab — provides method, path, and operationId for each operation.
   */
  @GetMapping("/{mockServerId}/config")
  public ResponseEntity<MockServerStandaloneConfigDto> getStandaloneConfig(
      @PathVariable UUID mockServerId) {
    return mockServerService
        .findById(mockServerId)
        .flatMap(
            server ->
                configRepository
                    .findByMockServerId(mockServerId)
                    .map(
                        cfg ->
                            new MockServerStandaloneConfigDto(
                                server.getDefaultStrategy(),
                                cfg.getSchemaValidationMode(),
                                cfg.getMaxVariantsPerOperation(),
                                cfg.getMaxTotalVariants(),
                                cfg.isMcpEnabled())))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{mockServerId}/config")
  public ResponseEntity<MockServerStandaloneConfigDto> patchStandaloneConfig(
      @PathVariable UUID mockServerId, @RequestBody Map<String, Object> body) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    MockServerConfigEntity cfg =
        configRepository
            .findByMockServerId(mockServerId)
            .orElseThrow(() -> new IllegalStateException("mock server config row missing"));
    applySchemaValidationModeFromPatchBody(cfg, body);
    if (body.containsKey("maxVariantsPerOperation")) {
      cfg.setMaxVariantsPerOperation(((Number) body.get("maxVariantsPerOperation")).intValue());
    }
    if (body.containsKey("maxTotalVariants")) {
      cfg.setMaxTotalVariants(((Number) body.get("maxTotalVariants")).intValue());
    }
    if (body.containsKey("mcpEnabled")) {
      cfg.setMcpEnabled(Boolean.TRUE.equals(body.get("mcpEnabled")));
    }
    configRepository.save(cfg);
    if (body.containsKey("defaultStrategy") && body.get("defaultStrategy") != null) {
      mockServerService.updateStrategy(
          mockServerId, MockResponseStrategy.valueOf((String) body.get("defaultStrategy")));
    }
    MockServerEntity server =
        mockServerService.findById(mockServerId).orElseThrow();
    MockServerConfigEntity saved =
        configRepository.findByMockServerId(mockServerId).orElseThrow();
    return ResponseEntity.ok(
        new MockServerStandaloneConfigDto(
            server.getDefaultStrategy(),
            saved.getSchemaValidationMode(),
            saved.getMaxVariantsPerOperation(),
            saved.getMaxTotalVariants(),
            saved.isMcpEnabled()));
  }

  @GetMapping("/{mockServerId}/spec-operations")
  public ResponseEntity<?> listSpecOperations(@PathVariable UUID mockServerId) {
    return mockServerService
        .findById(mockServerId)
        .map(
            server ->
                ResponseEntity.ok(
                    operationRepository.findBySpecId(server.getSpecId())))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{mockServerId}/export")
  public ResponseEntity<MockServerExportDto> exportMockServer(@PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mockServerService.exportMockServer(mockServerId));
  }

  // ── Environment variables (used in CEL expressions as env.KEY) ──────────

  @GetMapping("/{mockServerId}/env-vars")
  public ResponseEntity<List<MockServerEnvVarEntity>> listEnvVars(
      @PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(envVarRepository.findByMockServerId(mockServerId));
  }

  @PostMapping("/{mockServerId}/env-vars")
  public ResponseEntity<MockServerEnvVarEntity> createEnvVar(
      @PathVariable UUID mockServerId, @RequestBody Map<String, String> body) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    String key = body.get("varKey");
    String value = body.get("varValue");
    if (key == null || key.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    // Upsert: update if key already exists
    MockServerEnvVarEntity entity =
        envVarRepository
            .findByMockServerIdAndVarKey(mockServerId, key)
            .orElse(new MockServerEnvVarEntity(mockServerId, key, value));
    entity.setVarValue(value != null ? value : "");
    return ResponseEntity.status(HttpStatus.CREATED).body(envVarRepository.save(entity));
  }

  @PutMapping("/{mockServerId}/env-vars/{envVarId}")
  public ResponseEntity<MockServerEnvVarEntity> updateEnvVar(
      @PathVariable UUID mockServerId,
      @PathVariable UUID envVarId,
      @RequestBody Map<String, String> body) {
    return envVarRepository
        .findById(envVarId)
        .filter(e -> e.getMockServerId().equals(mockServerId))
        .map(
            e -> {
              if (body.containsKey("varValue")) e.setVarValue(body.get("varValue"));
              return ResponseEntity.ok(envVarRepository.save(e));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{mockServerId}/env-vars/{envVarId}")
  public ResponseEntity<Void> deleteEnvVar(
      @PathVariable UUID mockServerId, @PathVariable UUID envVarId) {
    if (envVarRepository.findById(envVarId)
        .filter(e -> e.getMockServerId().equals(mockServerId))
        .isEmpty()) {
      return ResponseEntity.<Void>notFound().build();
    }
    envVarRepository.deleteById(envVarId);
    return ResponseEntity.<Void>noContent().build();
  }

  // ── MCP config (enable/disable MCP for this server) ─────────────────────

  @GetMapping("/{mockServerId}/mcp-config")
  public ResponseEntity<MockServerConfigEntity> getMcpConfig(@PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return configRepository
        .findByMockServerId(mockServerId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{mockServerId}/mcp-config")
  public ResponseEntity<MockServerConfigEntity> patchMcpConfig(
      @PathVariable UUID mockServerId, @RequestBody Map<String, Object> body) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return configRepository
        .findByMockServerId(mockServerId)
        .map(
            config -> {
              if (body.containsKey("mcpEnabled")) {
                config.setMcpEnabled(Boolean.TRUE.equals(body.get("mcpEnabled")));
              }
              return ResponseEntity.ok(configRepository.save(config));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Accepts {@code schemaValidationMode} or legacy {@code validationMode}; maps {@code LENIENT} →
   * {@link SchemaValidationMode#WARN}.
   */
  private void applySchemaValidationModeFromPatchBody(
      MockServerConfigEntity cfg, Map<String, Object> body) {
    Object raw = body.get("schemaValidationMode");
    if (raw == null) {
      raw = body.get("validationMode");
    }
    if (raw == null) {
      return;
    }
    String s = raw instanceof String ? ((String) raw).trim() : String.valueOf(raw).trim();
    if (s.isEmpty()) {
      return;
    }
    if ("LENIENT".equalsIgnoreCase(s)) {
      s = "WARN";
    }
    String upper = s.toUpperCase();
    try {
      cfg.setSchemaValidationMode(SchemaValidationMode.valueOf(upper));
    } catch (IllegalArgumentException ex) {
      throw MockApiException.badRequest(
          "invalid_schema_validation_mode",
          "Invalid schemaValidationMode: use one of OFF, WARN, STRICT.",
          List.of("received: " + s));
    }
  }
}
