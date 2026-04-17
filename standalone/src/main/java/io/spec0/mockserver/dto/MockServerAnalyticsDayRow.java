package io.spec0.mockserver.dto;

import java.time.LocalDate;

/** Per-day bucket for {@link MockServerAnalyticsResponse}; aligns with cloud platform shape. */
public record MockServerAnalyticsDayRow(LocalDate date, int count, Double averageLatencyMs) {}
