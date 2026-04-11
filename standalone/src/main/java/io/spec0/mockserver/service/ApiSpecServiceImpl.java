package io.spec0.mockserver.service;

import io.spec0.mockserver.domain.ApiSpecEntity;
import io.spec0.mockserver.domain.MockServerOperationEntity;
import io.spec0.mockserver.openapi.validation.ParsedOpenApiCache;
import io.spec0.mockserver.port.ApiSpecServicePort;
import io.spec0.mockserver.repository.ApiSpecRepository;
import io.spec0.mockserver.repository.MockServerOperationRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApiSpecServiceImpl implements ApiSpecServicePort {

  private final ApiSpecRepository apiSpecRepository;
  private final MockServerOperationRepository operationRepository;
  private final ParsedOpenApiCache parsedOpenApiCache;

  @Override
  public ApiSpecEntity registerSpec(String specName, String specContent) {
    String hash = sha256(specContent);

    // Idempotent: return existing if same name + hash
    Optional<ApiSpecEntity> existing = apiSpecRepository.findBySpecNameAndSpecHash(specName, hash);
    if (existing.isPresent()) {
      log.info("Spec '{}' with same hash already registered, returning existing", specName);
      return existing.get();
    }

    String version = parseSpecVersion(specContent);
    ApiSpecEntity spec = new ApiSpecEntity(specName, specContent, hash, version);
    ApiSpecEntity saved = apiSpecRepository.save(spec);
    parsedOpenApiCache.invalidate(saved.getSpecId());

    List<MockServerOperationEntity> operations = parseOperations(saved.getSpecId(), specContent);
    operationRepository.saveAll(operations);

    log.info(
        "Registered spec '{}' (id={}) with {} operations",
        specName,
        saved.getSpecId(),
        operations.size());
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ApiSpecEntity> findById(UUID specId) {
    return apiSpecRepository.findById(specId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ApiSpecEntity> findByNameAndHash(String specName, String specHash) {
    return apiSpecRepository.findBySpecNameAndSpecHash(specName, specHash);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByNameAndHash(String specName, String specHash) {
    return apiSpecRepository.existsBySpecNameAndSpecHash(specName, specHash);
  }

  // --- Private helpers ---

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private String parseSpecVersion(String specContent) {
    try {
      ParseOptions opts = new ParseOptions();
      opts.setResolve(false);
      SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, opts);
      if (result.getOpenAPI() != null && result.getOpenAPI().getInfo() != null) {
        return result.getOpenAPI().getInfo().getVersion();
      }
    } catch (Exception e) {
      log.warn("Could not parse spec version: {}", e.getMessage());
    }
    return null;
  }

  private List<MockServerOperationEntity> parseOperations(UUID specId, String specContent) {
    List<MockServerOperationEntity> ops = new ArrayList<>();
    try {
      ParseOptions opts = new ParseOptions();
      opts.setResolve(true);
      SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, opts);
      OpenAPI openAPI = result.getOpenAPI();
      if (openAPI == null || openAPI.getPaths() == null) {
        log.warn("No paths found in spec {}", specId);
        return ops;
      }

      for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
        String path = pathEntry.getKey();
        PathItem item = pathEntry.getValue();

        addIfPresent(ops, specId, "GET", path, item.getGet());
        addIfPresent(ops, specId, "POST", path, item.getPost());
        addIfPresent(ops, specId, "PUT", path, item.getPut());
        addIfPresent(ops, specId, "DELETE", path, item.getDelete());
        addIfPresent(ops, specId, "PATCH", path, item.getPatch());
      }
    } catch (Exception e) {
      log.error("Error parsing operations from spec {}: {}", specId, e.getMessage());
    }
    return ops;
  }

  private void addIfPresent(
      List<MockServerOperationEntity> ops,
      UUID specId,
      String method,
      String path,
      Operation operation) {
    if (operation == null) return;
    String operationId =
        operation.getOperationId() != null
            ? operation.getOperationId()
            : synthesiseOperationId(method, path);
    String successStatus = findFirstSuccessStatus(operation);
    ops.add(new MockServerOperationEntity(specId, operationId, method, path, successStatus));
  }

  private String findFirstSuccessStatus(Operation operation) {
    if (operation.getResponses() == null) return "200";
    return operation.getResponses().keySet().stream()
        .filter(code -> code.startsWith("2"))
        .findFirst()
        .orElse("200");
  }

  static String synthesiseOperationId(String method, String path) {
    return method.toLowerCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
  }
}
