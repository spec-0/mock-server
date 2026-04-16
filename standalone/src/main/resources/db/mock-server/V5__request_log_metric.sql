CREATE TABLE IF NOT EXISTS mock_server.mock_request_log_metric (
    metric_id    UUID          NOT NULL PRIMARY KEY,
    log_id       UUID          NOT NULL REFERENCES mock_server.mock_request_logs (log_id) ON DELETE CASCADE,
    metric_key   VARCHAR(64)   NOT NULL,
    value_int    BIGINT,
    value_double DOUBLE,
    value_text   TEXT,
    UNIQUE (log_id, metric_key)
);

CREATE INDEX IF NOT EXISTS idx_mrlm_log_id ON mock_server.mock_request_log_metric (log_id);
CREATE INDEX IF NOT EXISTS idx_mrlm_metric_key ON mock_server.mock_request_log_metric (metric_key);
