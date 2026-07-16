-- Remaining workflow closure: report versions/reviews, task deadlines and in-app notifications.

ALTER TABLE `research_project_task`
    ADD COLUMN `deadline_at` DATETIME NULL AFTER `ended_at`;

CREATE TABLE `edu_experiment_record_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_id` BIGINT NOT NULL,
    `version_no` INT NOT NULL,
    `report_file_id` BIGINT NULL,
    `experiment_title` VARCHAR(200) NULL,
    `experiment_process` TEXT NULL,
    `experiment_result` TEXT NULL,
    `submitted_by` BIGINT NOT NULL,
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(50) NOT NULL DEFAULT 'submitted' COMMENT 'submitted/returned/graded/archived',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_experiment_record_version_record_no` (`record_id`, `version_no`),
    KEY `idx_experiment_record_version_record_status` (`record_id`, `status`)
) ENGINE=InnoDB COMMENT='Experiment report versions';

CREATE TABLE `edu_experiment_record_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_id` BIGINT NOT NULL,
    `version_id` BIGINT NULL,
    `action` VARCHAR(50) NOT NULL COMMENT 'submit/return/grade/archive/correct',
    `comment` VARCHAR(1000) NULL,
    `score` DECIMAL(6,2) NULL,
    `operator_id` BIGINT NOT NULL,
    `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_experiment_record_review_record_time` (`record_id`, `operated_at`)
) ENGINE=InnoDB COMMENT='Experiment report review history';

CREATE TABLE `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `recipient_id` BIGINT NOT NULL,
    `notification_type` VARCHAR(50) NOT NULL,
    `biz_type` VARCHAR(100) NULL,
    `biz_id` BIGINT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` VARCHAR(2000) NULL,
    `read_status` TINYINT NOT NULL DEFAULT 0,
    `read_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_notification_recipient_read_time` (`recipient_id`, `read_status`, `created_at`),
    KEY `idx_sys_notification_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB COMMENT='In-app notifications';

INSERT IGNORE INTO `auth_permission` (`permission_code`,`permission_name`,`permission_type`,`api_path`,`request_method`,`description`,`status`) VALUES
('edu:experiment-record:return','edu:experiment-record:return','api','/api/experiment-records/{id}/return','POST','Return experiment report','1'),
('edu:experiment-record:version','edu:experiment-record:version','api','/api/experiment-records/{id}/versions','POST','Submit report version','1'),
('edu:course:learning-summary','edu:course:learning-summary','api','/api/courses/{id}/learning-summary','GET','View course learning summary','1'),
('research:project:task','research:project:task','api','/api/research-projects/{id}/tasks','POST','Manage research tasks','1'),
('edu:training-plan:item','edu:training-plan:item','api','/api/training-plans/{id}/items','POST','Manage training items','1'),
('sys:notification:list','sys:notification:list','api','/api/notifications','GET','List notifications','1'),
('sys:notification:read','sys:notification:read','api','/api/notifications/{id}/read','POST','Read notification','1');

INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER','ADMIN') AND p.permission_code IN ('edu:experiment-record:return','edu:experiment-record:version','edu:course:learning-summary','research:project:task','edu:training-plan:item');
INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('STUDENT','TEACHER','ADMIN') AND p.permission_code IN ('sys:notification:list','sys:notification:read');
