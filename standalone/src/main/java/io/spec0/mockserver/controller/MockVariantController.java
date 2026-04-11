package io.spec0.mockserver.controller;

import io.spec0.mockserver.domain.MockRequestLogEntity;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import io.spec0.mockserver.dto.VariantCreateDto;
import io.spec0.mockserver.port.MockServerServicePort;
import io.spec0.mockserver.service.MockServerCoreServiceImpl;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock-server/servers/{mockServerId}")
@RequiredArgsConstructor
public class MockVariantController {

  private final MockServerServicePort mockServerService;
  private final MockServerCoreServiceImpl coreService;

  @GetMapping("/variants")
  public ResponseEntity<List<MockResponseVariantEntity>> listVariants(
      @PathVariable UUID mockServerId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mockServerService.getVariants(mockServerId));
  }

  @PostMapping("/variants")
  public ResponseEntity<MockResponseVariantEntity> createVariant(
      @PathVariable UUID mockServerId, @RequestBody VariantCreateDto dto) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    MockResponseVariantEntity created = mockServerService.createVariant(mockServerId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/variants/{variantId}")
  public ResponseEntity<MockResponseVariantEntity> updateVariant(
      @PathVariable UUID mockServerId,
      @PathVariable UUID variantId,
      @RequestBody VariantCreateDto dto) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mockServerService.updateVariant(mockServerId, variantId, dto));
  }

  @DeleteMapping("/variants/{variantId}")
  public ResponseEntity<Void> deleteVariant(
      @PathVariable UUID mockServerId, @PathVariable UUID variantId) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    mockServerService.deleteVariant(mockServerId, variantId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/logs")
  public ResponseEntity<List<MockRequestLogEntity>> getLogs(
      @PathVariable UUID mockServerId,
      @RequestParam(defaultValue = "50") int limit) {
    if (mockServerService.findById(mockServerId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(coreService.getRecentLogs(mockServerId, Math.min(limit, 200)));
  }
}
