-- Schema validation policy for OpenAPI request/response JSON (OFF / WARN / STRICT)

ALTER TABLE mock_server.mock_server_configs
  ADD COLUMN IF NOT EXISTS schema_validation_mode VARCHAR(16) NOT NULL DEFAULT 'OFF';
