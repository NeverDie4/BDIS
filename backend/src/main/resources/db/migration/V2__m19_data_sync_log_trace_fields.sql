ALTER TABLE log_data_sync
    ADD COLUMN task_id BIGINT NULL AFTER target_type,
    ADD COLUMN exchange_id BIGINT NULL AFTER task_id,
    ADD COLUMN business_type VARCHAR(100) NULL AFTER exchange_id,
    ADD COLUMN business_id BIGINT NULL AFTER business_type,
    ADD COLUMN external_no VARCHAR(100) NULL AFTER business_id,
    ADD KEY idx_log_data_sync_task_id (task_id),
    ADD KEY idx_log_data_sync_exchange_id (exchange_id),
    ADD KEY idx_log_data_sync_business (business_type, business_id),
    ADD KEY idx_log_data_sync_external_no (external_no);
