-- Strengthen file relation consistency and align audit indexes with supported filters.

DELETE duplicate_relation
FROM `sys_file_business` duplicate_relation
JOIN `sys_file_business` retained_relation
  ON retained_relation.file_id = duplicate_relation.file_id
 AND retained_relation.biz_type = duplicate_relation.biz_type
 AND retained_relation.biz_id = duplicate_relation.biz_id
 AND retained_relation.id < duplicate_relation.id;

ALTER TABLE `sys_file_business`
  ADD UNIQUE KEY `uk_sys_file_business_relation` (`file_id`, `biz_type`, `biz_id`);

ALTER TABLE `log_file_access`
  ADD COLUMN `failure_reason` VARCHAR(1000) NULL DEFAULT NULL
    COMMENT '访问失败原因' AFTER `result_status`,
  ADD KEY `idx_log_file_access_type_time` (`access_type`, `operation_time`),
  ADD KEY `idx_log_file_access_result_time` (`result_status`, `operation_time`);

ALTER TABLE `log_login`
  ADD KEY `idx_log_login_username_time` (`username`, `logged_in_at`),
  ADD KEY `idx_log_login_result_time` (`login_result`, `logged_in_at`);

ALTER TABLE `log_operation`
  ADD KEY `idx_log_operation_type_time` (`operation_type`, `operation_time`),
  ADD KEY `idx_log_operation_result_time` (`result_status`, `operation_time`);

ALTER TABLE `sys_file_resource`
  ADD KEY `idx_sys_file_resource_access_status` (`access_level`, `status`);
