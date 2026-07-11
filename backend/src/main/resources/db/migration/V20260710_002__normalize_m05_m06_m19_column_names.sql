ALTER TABLE `log_file_access`
  RENAME COLUMN `access_result` TO `result_status`;

ALTER TABLE `log_data_sync`
  DROP INDEX `idx_log_data_sync_business`;

ALTER TABLE `log_data_sync`
  RENAME COLUMN `business_type` TO `biz_type`,
  RENAME COLUMN `business_id` TO `biz_id`,
  ADD KEY `idx_log_data_sync_biz` (`biz_type`, `biz_id`);

ALTER TABLE `soap_sync_task`
  RENAME COLUMN `mock` TO `is_mock`;
