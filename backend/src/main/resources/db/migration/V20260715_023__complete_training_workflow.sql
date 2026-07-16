-- Training workflow extension: plan completion criteria, participant submissions and item progress.

ALTER TABLE `edu_training_plan`
    ADD COLUMN `completion_criteria` TEXT NULL COMMENT '培训完成标准';

ALTER TABLE `edu_training_record`
    ADD COLUMN `report_file_id` BIGINT NULL COMMENT '培训报告文件';

CREATE TABLE `edu_training_record_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `training_record_id` BIGINT NOT NULL,
    `plan_item_id` BIGINT NOT NULL,
    `progress` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    `completed` TINYINT NOT NULL DEFAULT 0,
    `started_at` DATETIME NULL,
    `completed_at` DATETIME NULL,
    `submitted_file_id` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `updated_by` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    `deleted_by` BIGINT NULL,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_record_item_record_item` (`training_record_id`, `plan_item_id`),
    KEY `idx_training_record_item_plan_item` (`plan_item_id`, `completed`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Training participant item completion';

CREATE TABLE `edu_training_record_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `training_record_id` BIGINT NOT NULL,
    `action` VARCHAR(50) NOT NULL COMMENT 'submit/return/approve/complete',
    `comment` VARCHAR(1000) NULL,
    `score` DECIMAL(6,2) NULL,
    `operator_id` BIGINT NOT NULL,
    `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_training_record_review_record_time` (`training_record_id`, `operated_at`)
) ENGINE=InnoDB COMMENT='Training record review history';

CREATE INDEX `idx_training_record_report_file` ON `edu_training_record` (`report_file_id`);

INSERT IGNORE INTO `auth_permission` (`permission_code`,`permission_name`,`permission_type`,`api_path`,`request_method`,`description`,`status`) VALUES
('edu:training-item:list','edu:training-item:list','api','/api/training-plans/{id}/items','GET','List training items','1'),
('edu:training-item:save','edu:training-item:save','api','/api/training-plans/{id}/items','POST','Save training item','1'),
('edu:training-record:item-progress','edu:training-record:item-progress','api','/api/training-records/{id}/items/{itemId}','PUT','Save training item progress','1'),
('edu:training-record:review','edu:training-record:review','api','/api/training-records/{id}/review','POST','Review training result','1');

INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER','ADMIN') AND p.permission_code IN ('edu:training-item:list','edu:training-item:save','edu:training-record:review');
INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('STUDENT','TEACHER','ADMIN') AND p.permission_code IN ('edu:training-item:list','edu:training-record:item-progress');
