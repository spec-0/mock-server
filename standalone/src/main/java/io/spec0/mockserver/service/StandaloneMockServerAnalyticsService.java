package io.spec0.mockserver.service;

import io.spec0.mockserver.dto.MockServerAnalyticsDayRow;
import io.spec0.mockserver.dto.MockServerAnalyticsResponse;
import io.spec0.mockserver.engine.model.MockRequestLogMetricKeys;
import io.spec0.mockserver.repository.MockRequestLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates mock request logs for the standalone admin UI — same metrics as cloud platform {@code
 * PlatformMockServerAnalyticsService}.
 */
@Service
@RequiredArgsConstructor
public class StandaloneMockServerAnalyticsService {

  private static final int DEFAULT_RANGE_DAYS = 30;

  private final MockRequestLogRepository logRepository;

  @Transactional(readOnly = true)
  public MockServerAnalyticsResponse aggregate(UUID mockServerId, LocalDate from, LocalDate to) {
    LocalDate endDate = to != null ? to : LocalDate.now(ZoneOffset.UTC);
    LocalDate startDate = from != null ? from : endDate.minusDays(DEFAULT_RANGE_DAYS - 1L);
    if (startDate.isAfter(endDate)) {
      LocalDate tmp = startDate;
      startDate = endDate;
      endDate = tmp;
    }

    LocalDateTime rangeStart = startDate.atStartOfDay();
    LocalDateTime rangeEndExclusive = endDate.plusDays(1).atStartOfDay();

    long total =
        logRepository.countByMockServerIdInRequestedAtRange(
            mockServerId, rangeStart, rangeEndExclusive);
    long errors =
        logRepository.countHttpErrorStatusInRange(mockServerId, rangeStart, rangeEndExclusive);
    int uniqueOps =
        Math.toIntExact(
            Math.min(
                logRepository.countDistinctOperationIdsInRange(
                    mockServerId, rangeStart, rangeEndExclusive),
                Integer.MAX_VALUE));

    double avgLatency = 0.0;
    Double avgFromDb =
        logRepository.averageIntMetricInRange(
            mockServerId,
            rangeStart,
            rangeEndExclusive,
            MockRequestLogMetricKeys.RESPONSE_LATENCY_MS);
    if (avgFromDb != null && !avgFromDb.isNaN()) {
      avgLatency = BigDecimal.valueOf(avgFromDb).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    double errorRate = 0.0;
    if (total > 0) {
      errorRate =
          BigDecimal.valueOf(errors)
              .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
              .doubleValue();
    }

    List<Object[]> rows =
        logRepository.aggregateByDay(
            mockServerId,
            rangeStart,
            rangeEndExclusive,
            MockRequestLogMetricKeys.RESPONSE_LATENCY_MS);
    List<MockServerAnalyticsDayRow> byDay = new ArrayList<>();
    for (Object[] row : rows) {
      if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
        continue;
      }
      LocalDate day =
          row[0] instanceof Date sqlDate
              ? sqlDate.toLocalDate()
              : row[0] instanceof LocalDate ld
                  ? ld
                  : ((java.sql.Timestamp) row[0]).toLocalDateTime().toLocalDate();
      long cnt = ((Number) row[1]).longValue();
      Double dayAvg = null;
      if (row.length >= 3 && row[2] != null) {
        double v = ((Number) row[2]).doubleValue();
        if (!Double.isNaN(v)) {
          dayAvg = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
      }
      byDay.add(
          new MockServerAnalyticsDayRow(
              day, Math.toIntExact(Math.min(cnt, Integer.MAX_VALUE)), dayAvg));
    }

    return new MockServerAnalyticsResponse(
        Math.toIntExact(Math.min(total, Integer.MAX_VALUE)),
        0,
        avgLatency,
        errorRate,
        uniqueOps,
        byDay);
  }
}
