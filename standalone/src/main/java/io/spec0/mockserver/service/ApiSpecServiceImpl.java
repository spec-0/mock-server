package io.spec0.mockserver.service;

import io.spec0.mockserver.domain.ApiSpecEntity;
import io.spec0.mockserver.engine.service.DefaultApiSpecRegistrationService;
import io.spec0.mockserver.openapi.validation.ParsedOpenApiCache;
import io.spec0.mockserver.port.ApiSpecServicePort;
import io.spec0.mockserver.repository.ApiSpecRepository;
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
  private final DefaultApiSpecRegistrationService registrationService;
  private final ParsedOpenApiCache parsedOpenApiCache;

  @Override
  public ApiSpecEntity registerSpec(String specName, String specContent) {
    var snapshot = registrationService.registerSpec(specName, specContent);
    parsedOpenApiCache.invalidate(snapshot.specId());
    return apiSpecRepository.findById(snapshot.specId()).orElseThrow();
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
}
