-- Complete research-project workflow: review, invitations, task assignment and submissions.

ALTER TABLE `research_project`
    ADD COLUMN `research_objective` TEXT NULL COMMENT '研究目标' AFTER `description`,
    ADD COLUMN `research_content` TEXT NULL COMMENT '研究内容' AFTER `research_objective`,
    ADD COLUMN `review_status` VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT 'draft/pending/approved/rejected' AFTER `project_status`,
    ADD COLUMN `review_comment` VARCHAR(1000) NULL,
    ADD COLUMN `reviewed_by` BIGINT NULL,
    ADD COLUMN `reviewed_at` DATETIME NULL,
    ADD COLUMN `archived_at` DATETIME NULL;

ALTER TABLE `rel_project_member`
    ADD COLUMN `invitation_status` VARCHAR(50) NOT NULL DEFAULT 'accepted' COMMENT 'pending/accepted/rejected' AFTER `member_status`,
    ADD COLUMN `invited_by` BIGINT NULL,
    ADD COLUMN `invited_at` DATETIME NULL,
    ADD COLUMN `accepted_at` DATETIME NULL,
    ADD COLUMN `rejected_at` DATETIME NULL,
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN `updated_by` BIGINT NULL,
    ADD COLUMN `version` INT NOT NULL DEFAULT 0;

CREATE TABLE `research_project_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `review_action` VARCHAR(50) NOT NULL COMMENT 'submit/approve/reject/archive',
    `from_status` VARCHAR(50) NULL,
    `to_status` VARCHAR(50) NOT NULL,
    `review_comment` VARCHAR(1000) NULL,
    `operator_id` BIGINT NOT NULL,
    `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_research_project_review_project_time` (`project_id`, `operated_at`)
) ENGINE=InnoDB COMMENT='Research project review history';

CREATE TABLE `research_project_task_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `member_role` VARCHAR(50) NOT NULL DEFAULT 'executor',
    `assigned_by` BIGINT NULL,
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `updated_by` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    `deleted_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_research_task_member_task_user` (`task_id`, `user_id`),
    KEY `idx_research_task_member_user_status` (`user_id`, `status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Research task members';

CREATE TABLE `research_project_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `task_id` BIGINT NULL,
    `record_id` BIGINT NULL COMMENT 'Optional experiment record source',
    `submitter_id` BIGINT NOT NULL,
    `submission_type` VARCHAR(50) NOT NULL COMMENT 'stage_report/data/sample/identification/statistics/paper/patent/final_result',
    `submission_title` VARCHAR(200) NOT NULL,
    `content` TEXT NULL,
    `file_id` BIGINT NULL,
    `submission_version` INT NOT NULL DEFAULT 1,
    `submission_status` VARCHAR(50) NOT NULL DEFAULT 'submitted' COMMENT 'submitted/reviewing/returned/approved/archived',
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` DATETIME NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `updated_by` BIGINT NULL,
    `deleted_at` DATETIME NULL,
    `deleted_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_research_submission_project_status` (`project_id`, `submission_status`, `is_deleted`),
    KEY `idx_research_submission_submitter` (`submitter_id`, `submission_status`),
    KEY `idx_research_submission_task` (`task_id`, `submission_status`)
) ENGINE=InnoDB COMMENT='Research project submissions';

CREATE TABLE `research_project_submission_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `submission_id` BIGINT NOT NULL,
    `review_action` VARCHAR(50) NOT NULL COMMENT 'return/approve/archive',
    `review_comment` VARCHAR(1000) NULL,
    `score` DECIMAL(6,2) NULL,
    `reviewer_id` BIGINT NOT NULL,
    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_research_submission_review_submission_time` (`submission_id`, `reviewed_at`)
) ENGINE=InnoDB COMMENT='Research submission review history';

INSERT IGNORE INTO `auth_permission` (`permission_code`,`permission_name`,`permission_type`,`api_path`,`request_method`,`description`,`status`) VALUES
('research:project:review','research:project:review','api','/api/research-projects/{id}/review','POST','Review research project','1'),
('research:project:invitation','research:project:invitation','api','/api/research-projects/{id}/invitations','POST','Invite project member','1'),
('research:project:invitation:respond','research:project:invitation:respond','api','/api/research-projects/{id}/invitations/respond','POST','Accept or reject invitation','1'),
('research:project:submission:add','research:project:submission:add','api','/api/research-projects/{id}/submissions','POST','Submit research stage result','1'),
('research:project:submission:list','research:project:submission:list','api','/api/research-projects/{id}/submissions','GET','List research submissions','1'),
('research:project:submission:review','research:project:submission:review','api','/api/research-projects/submissions/{id}/review','POST','Review research submission','1');

INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('ADMIN','REVIEWER') AND p.permission_code='research:project:review';
INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER','ADMIN') AND p.permission_code IN ('research:project:invitation','research:project:submission:list','research:project:submission:review');
INSERT IGNORE INTO `rel_role_permission` (`role_id`,`permission_id`)
SELECT r.id,p.id FROM `auth_role` r CROSS JOIN `auth_permission` p
WHERE r.role_code='STUDENT' AND p.permission_code IN ('research:project:invitation:respond','research:project:submission:add','research:project:submission:list');
