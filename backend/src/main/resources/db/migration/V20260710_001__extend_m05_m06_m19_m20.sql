ALTER TABLE `sys_file_resource`
  ADD COLUMN `storage_path` VARCHAR(500) NULL DEFAULT NULL COMMENT '文件在存储介质中的相对路径' AFTER `file_url`,
  ADD COLUMN `content_type` VARCHAR(150) NULL DEFAULT NULL COMMENT '文件 MIME 类型' AFTER `storage_type`,
  ADD COLUMN `uploader_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '上传人名称快照' AFTER `uploader_id`;

UPDATE `sys_file_resource`
SET `storage_path` = SUBSTRING(`file_url`, CHAR_LENGTH('/api/files/') + 1)
WHERE `storage_path` IS NULL
  AND `file_url` LIKE '/api/files/%';

ALTER TABLE `log_operation`
  ADD COLUMN `biz_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '业务对象类型' AFTER `operation_type`,
  ADD COLUMN `biz_id` BIGINT NULL DEFAULT NULL COMMENT '业务对象 ID' AFTER `biz_type`,
  ADD KEY `idx_log_operation_biz` (`biz_type`, `biz_id`);

ALTER TABLE `log_file_access`
  ADD COLUMN `operator_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人名称快照' AFTER `operator_id`,
  ADD COLUMN `access_result` VARCHAR(50) NULL DEFAULT NULL COMMENT '访问结果' AFTER `access_type`;

ALTER TABLE `log_data_sync`
  ADD COLUMN `task_id` BIGINT NULL DEFAULT NULL COMMENT 'SOAP 同步任务 ID' AFTER `target_id`,
  ADD COLUMN `exchange_id` BIGINT NULL DEFAULT NULL COMMENT 'SOAP 交换记录 ID' AFTER `task_id`,
  ADD COLUMN `business_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '导入业务对象类型' AFTER `exchange_id`,
  ADD COLUMN `business_id` BIGINT NULL DEFAULT NULL COMMENT '导入业务对象 ID' AFTER `business_type`,
  ADD COLUMN `external_no` VARCHAR(100) NULL DEFAULT NULL COMMENT '外部系统业务编号' AFTER `business_id`,
  ADD COLUMN `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功记录数' AFTER `external_no`,
  ADD COLUMN `failure_count` INT NOT NULL DEFAULT 0 COMMENT '失败记录数' AFTER `success_count`,
  ADD COLUMN `operator_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人名称快照' AFTER `operator_id`,
  ADD KEY `idx_log_data_sync_task_id` (`task_id`),
  ADD KEY `idx_log_data_sync_exchange_id` (`exchange_id`),
  ADD KEY `idx_log_data_sync_business` (`business_type`, `business_id`),
  ADD KEY `idx_log_data_sync_external_no` (`external_no`);

ALTER TABLE `soap_sync_task`
  ADD COLUMN `resource_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '交换资源类型' AFTER `task_name`,
  ADD COLUMN `mock` TINYINT NOT NULL DEFAULT 1 COMMENT '是否使用模拟 SOAP 响应' AFTER `sync_status`;

ALTER TABLE `soap_exchange_record`
  ADD COLUMN `parsed_payload` LONGTEXT NULL COMMENT 'SOAP 响应解析后的 JSON 数据' AFTER `error_message`,
  ADD COLUMN `called_by_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '调用人名称快照' AFTER `called_by`;
