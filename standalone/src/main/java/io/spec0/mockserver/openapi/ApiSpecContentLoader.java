package io.spec0.mockserver.openapi;

import io.spec0.mockserver.openapi.validation.SpecContentLoader;
import io.spec0.mockserver.repository.ApiSpecRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiSpecContentLoader implements SpecContentLoader {

  private final ApiSpecRepository apiSpecRepository;

  @Override
  public Optional<String> loadContent(UUID specId) {
    return apiSpecRepository.findById(specId).map(s -> s.getSpecContent());
  }
}
