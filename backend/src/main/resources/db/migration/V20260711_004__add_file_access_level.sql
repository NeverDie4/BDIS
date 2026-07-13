SET @ddl = IF(
  NOT EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_file_resource'
      AND column_name = 'access_level'
  ),
  'ALTER TABLE `sys_file_resource` ADD COLUMN `access_level` VARCHAR(20) NOT NULL DEFAULT ''private'' COMMENT ''访问级别：private、public'' AFTER `storage_type`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  NOT EXISTS(
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_file_resource'
      AND index_name = 'idx_sys_file_resource_access_level'
  ),
  'ALTER TABLE `sys_file_resource` ADD KEY `idx_sys_file_resource_access_level` (`access_level`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
