CREATE TABLE `edu_training_record_report_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `training_record_id` BIGINT NOT NULL,
  `version_no` INT NOT NULL,
  `file_id` BIGINT NULL,
  `content` TEXT NULL,
  `report_status` VARCHAR(50) NOT NULL DEFAULT 'submitted',
  `submitted_by` BIGINT NOT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_training_report_version` (`training_record_id`,`version_no`),
  KEY `idx_training_report_record_status` (`training_record_id`,`report_status`)
) ENGINE=InnoDB COMMENT='Training report versions';

INSERT IGNORE INTO auth_permission(permission_code,permission_name,permission_type,api_path,request_method,description,status) VALUES
('edu:course:learning-summary','edu:course:learning-summary','api','/api/courses/{id}/learning-summary','GET','Course learning summary','1'),
('edu:training-record:submit','edu:training-record:submit','api','/api/training-records/{id}/submit','POST','Submit training report','1'),
('edu:training-record:review','edu:training-record:review','api','/api/training-records/{id}/review','POST','Review training record','1'),
('edu:training-record:evaluation','edu:training-record:evaluation','api','/api/training-records/{id}/evaluations','POST','Evaluate training record','1');
INSERT IGNORE INTO rel_role_permission(role_id,permission_id) SELECT r.id,p.id FROM auth_role r CROSS JOIN auth_permission p WHERE r.role_code IN ('TEACHER','ADMIN') AND p.permission_code IN ('edu:training-record:review','edu:training-record:evaluation');
INSERT IGNORE INTO rel_role_permission(role_id,permission_id) SELECT r.id,p.id FROM auth_role r CROSS JOIN auth_permission p WHERE r.role_code IN ('STUDENT','TEACHER','ADMIN') AND p.permission_code='edu:training-record:submit';
