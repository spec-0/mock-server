package io.spec0.mockserver.engine.model;

/** Well-known keys for {@link MockRequestLogMetric} rows (extensible over time). */
public final class MockRequestLogMetricKeys {

  /** Wall-clock mock dispatch duration in milliseconds (see {@link MockRequestLogMetric#latencyMs}). */
  public static final String RESPONSE_LATENCY_MS = "RESPONSE_LATENCY_MS";

  private MockRequestLogMetricKeys() {}
}
