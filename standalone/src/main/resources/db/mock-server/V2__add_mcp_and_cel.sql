-- V2: Add MCP enable flag, CEL expression support, and per-server env vars

-- CEL expression column on variants (null = static, non-null = CEL variant)
ALTER TABLE mock_server.mock_response_variants
  ADD COLUMN IF NOT EXISTS cel_expression TEXT;

-- MCP enabled flag on server configs (application-level toggle, per mock-server)
ALTER TABLE mock_server.mock_server_configs
  ADD COLUMN IF NOT EXISTS mcp_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Per-server environment variables accessible in CEL expressions as env.<KEY>
CREATE TABLE IF NOT EXISTS mock_server.mock_server_env_vars (
  env_var_id     UUID          NOT NULL PRIMARY KEY,
  mock_server_id UUID          NOT NULL,
  var_key        VARCHAR(255)  NOT NULL,
  var_value      TEXT          NOT NULL,
  created_at     TIMESTAMP     NOT NULL,
  CONSTRAINT fk_env_vars_server
    FOREIGN KEY (mock_server_id)
    REFERENCES mock_server.mock_servers (mock_server_id) ON DELETE CASCADE,
  CONSTRAINT uq_env_var_key UNIQUE (mock_server_id, var_key)
);

CREATE INDEX IF NOT EXISTS idx_mev_server_id ON mock_server.mock_server_env_vars (mock_server_id);
