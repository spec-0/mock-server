package io.spec0.mockserver.repository;

import io.spec0.mockserver.domain.MockRequestLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MockRequestLogRepository extends JpaRepository<MockRequestLogEntity, UUID> {

  List<MockRequestLogEntity> findByMockServerIdOrderByRequestedAtDesc(
      UUID mockServerId, Pageable pageable);

  void deleteByMockServerId(UUID mockServerId);

  @Query(
      "SELECT COUNT(e) FROM MockRequestLogEntity e WHERE e.mockServerId = :mockServerId AND"
          + " e.requestedAt >= :start AND e.requestedAt < :end")
  long countByMockServerIdInRequestedAtRange(
      @Param("mockServerId") UUID mockServerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query(
      "SELECT COUNT(e) FROM MockRequestLogEntity e WHERE e.mockServerId = :mockServerId AND"
          + " e.requestedAt >= :start AND e.requestedAt < :end AND LENGTH(e.responseStatusCode) = 3"
          + " AND SUBSTRING(e.responseStatusCode, 1, 1) IN ('4', '5')")
  long countHttpErrorStatusInRange(
      @Param("mockServerId") UUID mockServerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query(
      value =
          "SELECT CAST(l.requested_at AS DATE) AS day_bucket, "
              + "COUNT(l.log_id) AS cnt, "
              + "CAST(AVG(m.value_int) AS DOUBLE) AS avg_ms "
              + "FROM mock_server.mock_request_logs l "
              + "LEFT JOIN mock_server.mock_request_log_metric m ON m.log_id = l.log_id "
              + "AND m.metric_key = :metricKey "
              + "WHERE l.mock_server_id = :mockServerId "
              + "AND l.requested_at >= :start AND l.requested_at < :end "
              + "GROUP BY CAST(l.requested_at AS DATE) "
              + "ORDER BY day_bucket",
      nativeQuery = true)
  List<Object[]> aggregateByDay(
      @Param("mockServerId") UUID mockServerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("metricKey") String metricKey);

  @Query(
      value =
          "SELECT CAST(AVG(m.value_int) AS DOUBLE) FROM mock_server.mock_request_log_metric m "
              + "INNER JOIN mock_server.mock_request_logs l ON l.log_id = m.log_id "
              + "WHERE m.metric_key = :metricKey AND l.mock_server_id = :mockServerId "
              + "AND l.requested_at >= :start AND l.requested_at < :end",
      nativeQuery = true)
  Double averageIntMetricInRange(
      @Param("mockServerId") UUID mockServerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("metricKey") String metricKey);

  @Query(
      "SELECT COUNT(DISTINCT e.operationId) FROM MockRequestLogEntity e WHERE e.mockServerId ="
          + " :mockServerId AND e.requestedAt >= :start AND e.requestedAt < :end AND e.operationId"
          + " IS NOT NULL AND LENGTH(e.operationId) > 0")
  long countDistinctOperationIdsInRange(
      @Param("mockServerId") UUID mockServerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
