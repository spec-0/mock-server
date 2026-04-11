package io.spec0.mockserver.dto;

import io.spec0.mockserver.domain.MockResponseStrategy;
import io.spec0.mockserver.openapi.validation.SchemaValidationMode;

/** Combined mock-server + config fields exposed to the standalone UI. */
public record MockServerStandaloneConfigDto(
    MockResponseStrategy defaultStrategy,
    SchemaValidationMode schemaValidationMode,
    int maxVariantsPerOperation,
    int maxTotalVariants,
    boolean mcpEnabled) {}
