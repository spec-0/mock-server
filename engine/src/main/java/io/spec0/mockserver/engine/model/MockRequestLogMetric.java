package io.spec0.mockserver.engine.model;

/**
 * One named metric attached to a {@link MockRequestLog}. Stored in a child table so new kinds of
 * metrics can be added without widening the log row for every deployment.
 */
public record MockRequestLogMetric(
    String key, Long intValue, Double doubleValue, String textValue) {

  public static MockRequestLogMetric latencyMs(long millis) {
    return new MockRequestLogMetric(
        MockRequestLogMetricKeys.RESPONSE_LATENCY_MS, millis, null, null);
  }
}
