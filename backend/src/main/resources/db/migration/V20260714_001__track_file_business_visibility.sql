ALTER TABLE `sys_file_business`
    ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '该业务关系是否要求文件公开访问' AFTER `file_usage`,
    ADD KEY `idx_sys_file_business_public` (`file_id`, `is_public`);

UPDATE `sys_file_business` relation_record
JOIN `sys_file_resource` file_resource
  ON file_resource.id = relation_record.file_id
SET relation_record.is_public = 1
WHERE file_resource.access_level = 'public'
  AND file_resource.is_deleted = 0;
