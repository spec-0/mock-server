package io.spec0.mockserver.controller;

import io.spec0.mockserver.domain.ApiSpecEntity;
import io.spec0.mockserver.port.ApiSpecServicePort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock-server/specs")
@RequiredArgsConstructor
public class ApiSpecController {

  private final ApiSpecServicePort apiSpecService;

  @PostMapping
  public ResponseEntity<ApiSpecEntity> registerSpec(@RequestBody Map<String, String> body) {
    String specName = body.get("specName");
    String specContent = body.get("specContent");
    if (specName == null || specContent == null) {
      return ResponseEntity.badRequest().build();
    }
    ApiSpecEntity spec = apiSpecService.registerSpec(specName, specContent);
    return ResponseEntity.status(HttpStatus.CREATED).body(spec);
  }

  @GetMapping("/{specId}")
  public ResponseEntity<ApiSpecEntity> getSpec(@PathVariable UUID specId) {
    return apiSpecService
        .findById(specId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
