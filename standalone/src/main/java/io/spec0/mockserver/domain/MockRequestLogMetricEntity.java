package io.spec0.mockserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "mock_request_log_metric",
    schema = "mock_server",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_mock_request_log_metric_log_key",
            columnNames = {"log_id", "metric_key"}),
    indexes = {
      @Index(name = "idx_mrlm_log_id", columnList = "log_id"),
      @Index(name = "idx_mrlm_metric_key", columnList = "metric_key")
    })
@Getter
@Setter
@NoArgsConstructor
public class MockRequestLogMetricEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "metric_id")
  private UUID metricId;

  @Column(name = "log_id", nullable = false)
  private UUID logId;

  @Column(name = "metric_key", nullable = false, length = 64)
  private String metricKey;

  @Column(name = "value_int")
  private Long valueInt;

  @Column(name = "value_double")
  private Double valueDouble;

  @Column(name = "value_text")
  private String valueText;

  public MockRequestLogMetricEntity(
      UUID logId, String metricKey, Long valueInt, Double valueDouble, String valueText) {
    this.logId = logId;
    this.metricKey = metricKey;
    this.valueInt = valueInt;
    this.valueDouble = valueDouble;
    this.valueText = valueText;
  }
}
