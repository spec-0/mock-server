ALTER TABLE mock_server.mock_response_variants
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255) NOT NULL DEFAULT '*/*';

CREATE INDEX IF NOT EXISTS idx_mrv_server_op_status_ct
    ON mock_server.mock_response_variants (mock_server_id, operation_id, status_code, content_type);
