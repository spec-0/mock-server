package io.spec0.mockserver.dto;

import java.util.List;

/**
 * JSON for GET /mock-server/servers/{id}/analytics — same fields as platform {@code
 * MockServerAnalytics} (OpenAPI) so the shared UI tab works unchanged.
 */
public record MockServerAnalyticsResponse(
    int totalRequests,
    int uniqueUsers,
    double averageResponseTime,
    double errorRate,
    /** Distinct non-empty operationIds in range (overview “unique operations”). */
    int uniqueOperations,
    List<MockServerAnalyticsDayRow> requestsByDay) {}
