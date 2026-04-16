package io.spec0.mockserver.engine.spi;

import java.util.Map;
import java.util.UUID;

/** Outbound port for per-server environment variables used by CEL expressions. */
public interface MockServerEnvVarPort {
  Map<String, String> findEnvVarsByMockServerId(UUID mockServerId);
}
