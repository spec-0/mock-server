-- Mock Server Core schema
-- Compatible with both H2 (standalone) and PostgreSQL (platform).
-- UUID generation is handled by JPA/Hibernate before INSERT — no DB-side DEFAULT needed.

CREATE SCHEMA IF NOT EXISTS mock_server;

CREATE TABLE IF NOT EXISTS mock_server.api_specs (
    spec_id      UUID          NOT NULL PRIMARY KEY,
    spec_name    VARCHAR(255)  NOT NULL,
    spec_content TEXT          NOT NULL,
    spec_hash    VARCHAR(64)   NOT NULL,
    spec_version VARCHAR(100),
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    UNIQUE (spec_name, spec_hash)
);

CREATE TABLE IF NOT EXISTS mock_server.mock_server_operations (
    id                  UUID          NOT NULL PRIMARY KEY,
    spec_id             UUID          NOT NULL REFERENCES mock_server.api_specs (spec_id),
    operation_id        VARCHAR(255)  NOT NULL,
    http_method         VARCHAR(10)   NOT NULL,
    path                VARCHAR(1000) NOT NULL,
    success_status_code VARCHAR(10)   NOT NULL DEFAULT '200',
    created_at          TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ms_ops_spec_id ON mock_server.mock_server_operations (spec_id);

CREATE TABLE IF NOT EXISTS mock_server.mock_servers (
    mock_server_id   UUID         NOT NULL PRIMARY KEY,
    spec_id          UUID         NOT NULL REFERENCES mock_server.api_specs (spec_id),
    name             VARCHAR(255) NOT NULL,
    api_key_hash     VARCHAR(255),
    api_key_preview  VARCHAR(20),
    default_strategy VARCHAR(30)  NOT NULL DEFAULT 'RANDOM',
    is_enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS mock_server.mock_server_configs (
    config_id                  UUID    NOT NULL PRIMARY KEY,
    mock_server_id             UUID    NOT NULL UNIQUE REFERENCES mock_server.mock_servers (mock_server_id),
    max_variants_per_operation INTEGER NOT NULL DEFAULT 10,
    max_total_variants         INTEGER NOT NULL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS mock_server.mock_response_variants (
    variant_id     UUID          NOT NULL PRIMARY KEY,
    mock_server_id UUID          NOT NULL REFERENCES mock_server.mock_servers (mock_server_id),
    operation_id   VARCHAR(255)  NOT NULL,
    response_name  VARCHAR(255)  NOT NULL,
    status_code    VARCHAR(10)   NOT NULL,
    response_body  TEXT,
    headers        TEXT,
    is_default     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_generated   BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order  INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mrv_server_op ON mock_server.mock_response_variants (mock_server_id, operation_id);
CREATE INDEX IF NOT EXISTS idx_mrv_server_default ON mock_server.mock_response_variants (mock_server_id, is_default);

CREATE TABLE IF NOT EXISTS mock_server.mock_operation_configs (
    config_id            UUID          NOT NULL PRIMARY KEY,
    mock_server_id       UUID          NOT NULL REFERENCES mock_server.mock_servers (mock_server_id),
    operation_id         VARCHAR(255)  NOT NULL,
    is_enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    strategy_override    VARCHAR(30),
    round_robin_position INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_moc_server_op ON mock_server.mock_operation_configs (mock_server_id, operation_id);

CREATE TABLE IF NOT EXISTS mock_server.mock_request_logs (
    log_id               UUID          NOT NULL PRIMARY KEY,
    mock_server_id       UUID          NOT NULL REFERENCES mock_server.mock_servers (mock_server_id),
    operation_id         VARCHAR(255),
    request_path         VARCHAR(2000) NOT NULL,
    request_method       VARCHAR(10)   NOT NULL,
    response_status_code VARCHAR(10),
    variant_id           UUID,
    requested_at         TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mrl_server_id ON mock_server.mock_request_logs (mock_server_id);
CREATE INDEX IF NOT EXISTS idx_mrl_requested_at ON mock_server.mock_request_logs (requested_at);
