package io.spec0.mockserver.adapter;

import io.spec0.mockserver.domain.ApiSpecEntity;
import io.spec0.mockserver.domain.MockServerOperationEntity;
import io.spec0.mockserver.engine.model.ApiSpecSnapshot;
import io.spec0.mockserver.engine.model.MockServerOperation;
import io.spec0.mockserver.engine.spi.ApiSpecCatalogPersistencePort;
import io.spec0.mockserver.repository.ApiSpecRepository;
import io.spec0.mockserver.repository.MockServerOperationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaApiSpecCatalogAdapter implements ApiSpecCatalogPersistencePort {

  private final ApiSpecRepository apiSpecRepository;
  private final MockServerOperationRepository operationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<ApiSpecSnapshot> findByNameAndHash(String specName, String specHash) {
    return apiSpecRepository.findBySpecNameAndSpecHash(specName, specHash).map(this::toSnapshot);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ApiSpecSnapshot> findById(UUID specId) {
    return apiSpecRepository.findById(specId).map(this::toSnapshot);
  }

  @Override
  @Transactional
  public ApiSpecSnapshot saveNewSpecWithOperations(
      String specName,
      String specContent,
      String specHash,
      String specVersion,
      List<MockServerOperation> operations) {
    ApiSpecEntity saved =
        apiSpecRepository.save(new ApiSpecEntity(specName, specContent, specHash, specVersion));
    UUID specId = saved.getSpecId();
    List<MockServerOperationEntity> rows =
        operations.stream()
            .map(
                o ->
                    new MockServerOperationEntity(
                        specId,
                        o.getOperationId(),
                        o.getHttpMethod(),
                        o.getPath(),
                        o.getSuccessStatusCode()))
            .toList();
    operationRepository.saveAll(rows);
    return toSnapshot(saved);
  }

  private ApiSpecSnapshot toSnapshot(ApiSpecEntity e) {
    return new ApiSpecSnapshot(
        e.getSpecId(), e.getSpecName(), e.getSpecContent(), e.getSpecHash(), e.getSpecVersion());
  }
}
