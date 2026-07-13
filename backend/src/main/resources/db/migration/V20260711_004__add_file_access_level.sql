ALTER TABLE `sys_file_resource`
  ADD COLUMN `access_level` VARCHAR(20) NOT NULL DEFAULT 'private'
    COMMENT '访问级别：private、public' AFTER `storage_type`,
  ADD KEY `idx_sys_file_resource_access_level` (`access_level`);
