package io.spec0.mockserver.dto;

import io.spec0.mockserver.domain.MockResponseVariantEntity;
import java.util.List;

public record VariantSaveResult(
    MockResponseVariantEntity entity, List<String> validationWarnings) {}
