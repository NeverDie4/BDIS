-- Unify course, research project, base and herb relationships.
-- Reuses existing master tables: edu_course, research_project, herb_base,
-- herb_species, edu_training_plan and edu_experiment_record.
-- Relationship tables intentionally do not add physical foreign keys because
-- existing BDIS migrations use application-level soft-delete/access guards.

-- A submitted experiment report is one canonical file. Additional images and
-- process attachments continue to use sys_file_business.
ALTER TABLE `edu_experiment_record`
    ADD COLUMN `report_file_id` BIGINT NULL DEFAULT NULL COMMENT 'Canonical single-file experiment report';

ALTER TABLE `edu_training_record`
    ADD COLUMN `completion_proof_file_id` BIGINT NULL DEFAULT NULL COMMENT 'Completion proof file';

-- Course-level resource associations.
CREATE TABLE `rel_course_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `base_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'practice' COMMENT 'practice/reference/optional',
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_course_base_course_base` (`course_id`, `base_id`),
    KEY `idx_rel_course_base_base_id` (`base_id`),
    KEY `idx_rel_course_base_status` (`status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Course and practice-base relationship';

CREATE TABLE `rel_course_species` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `species_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'material' COMMENT 'material/reference/optional',
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_course_species_course_species` (`course_id`, `species_id`),
    KEY `idx_rel_course_species_species_id` (`species_id`),
    KEY `idx_rel_course_species_status` (`status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Course and herb-species relationship';

-- Step-level required resources.
CREATE TABLE `rel_experiment_step_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `step_id` BIGINT NOT NULL,
    `base_id` BIGINT NOT NULL,
    `usage_note` VARCHAR(500) NULL,
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_step_base_step_base` (`step_id`, `base_id`),
    KEY `idx_rel_step_base_base_id` (`base_id`)
) ENGINE=InnoDB COMMENT='Experiment step and required base relationship';

CREATE TABLE `rel_experiment_step_species` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `step_id` BIGINT NOT NULL,
    `species_id` BIGINT NOT NULL,
    `usage_note` VARCHAR(500) NULL,
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_step_species_step_species` (`step_id`, `species_id`),
    KEY `idx_rel_step_species_species_id` (`species_id`)
) ENGINE=InnoDB COMMENT='Experiment step and required herb relationship';

-- Research project associations.
CREATE TABLE `rel_project_course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'foundation' COMMENT 'foundation/method/data_source',
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_project_course_project_course` (`project_id`, `course_id`),
    KEY `idx_rel_project_course_course_id` (`course_id`),
    KEY `idx_rel_project_course_status` (`status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Research project and experiment course relationship';

CREATE TABLE `rel_project_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `base_id` BIGINT NOT NULL,
    `usage_type` VARCHAR(50) NOT NULL DEFAULT 'research' COMMENT 'research/collection/practice',
    `permission_status` VARCHAR(50) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    `started_at` DATETIME NULL,
    `ended_at` DATETIME NULL,
    `approved_by` BIGINT NULL,
    `approved_at` DATETIME NULL,
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
    UNIQUE KEY `uk_rel_project_base_project_base` (`project_id`, `base_id`),
    KEY `idx_rel_project_base_base_id` (`base_id`),
    KEY `idx_rel_project_base_permission` (`permission_status`, `status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Research project and base usage relationship';

CREATE TABLE `rel_project_species` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `species_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'research_object' COMMENT 'research_object/sample/reference',
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_rel_project_species_project_species` (`project_id`, `species_id`),
    KEY `idx_rel_project_species_species_id` (`species_id`),
    KEY `idx_rel_project_species_status` (`status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Research project and herb-species relationship';

-- Reusable research tasks and their course/herb sources.
CREATE TABLE `research_project_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL,
    `task_no` VARCHAR(64) NOT NULL,
    `task_name` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `responsible_user_id` BIGINT NULL,
    `base_id` BIGINT NULL COMMENT 'One primary base for this task',
    `source_record_id` BIGINT NULL COMMENT 'One source experiment/data record',
    `started_at` DATETIME NULL,
    `ended_at` DATETIME NULL,
    `task_status` VARCHAR(50) NOT NULL DEFAULT 'pending' COMMENT 'pending/in_progress/submitted/returned/completed',
    `sort_order` INT NOT NULL DEFAULT 0,
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
    UNIQUE KEY `uk_research_project_task_project_task_no` (`project_id`, `task_no`),
    KEY `idx_research_project_task_project_status` (`project_id`, `task_status`, `status`, `is_deleted`),
    KEY `idx_research_project_task_responsible_user` (`responsible_user_id`),
    KEY `idx_research_project_task_base_id` (`base_id`),
    KEY `idx_research_project_task_source_record` (`source_record_id`)
) ENGINE=InnoDB COMMENT='Research project task';

CREATE TABLE `rel_research_task_course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'method',
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_research_task_course_task_course` (`task_id`, `course_id`),
    KEY `idx_rel_research_task_course_course_id` (`course_id`)
) ENGINE=InnoDB COMMENT='Research task and course relationship';

CREATE TABLE `rel_research_task_species` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `species_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'sample',
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_research_task_species_task_species` (`task_id`, `species_id`),
    KEY `idx_rel_research_task_species_species_id` (`species_id`)
) ENGINE=InnoDB COMMENT='Research task and herb relationship';

-- Training plans support multiple courses/projects/bases/herbs while keeping
-- edu_training_plan.course_id for backward compatibility with existing code.
CREATE TABLE `rel_training_plan_course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_training_plan_course_plan_course` (`plan_id`, `course_id`),
    KEY `idx_rel_training_plan_course_course_id` (`course_id`)
) ENGINE=InnoDB COMMENT='Training plan and course relationship';

INSERT INTO `rel_training_plan_course` (`plan_id`, `course_id`, `is_primary`)
SELECT `id`, `course_id`, 1
FROM `edu_training_plan`
WHERE `course_id` IS NOT NULL;

CREATE TABLE `rel_training_plan_project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL,
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_training_plan_project_plan_project` (`plan_id`, `project_id`),
    KEY `idx_rel_training_plan_project_project_id` (`project_id`)
) ENGINE=InnoDB COMMENT='Training plan and research project relationship';

CREATE TABLE `rel_training_plan_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `base_id` BIGINT NOT NULL,
    `usage_type` VARCHAR(50) NOT NULL DEFAULT 'practice',
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_training_plan_base_plan_base` (`plan_id`, `base_id`),
    KEY `idx_rel_training_plan_base_base_id` (`base_id`)
) ENGINE=InnoDB COMMENT='Training plan and base relationship';

CREATE TABLE `rel_training_plan_species` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `species_id` BIGINT NOT NULL,
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_training_plan_species_plan_species` (`plan_id`, `species_id`),
    KEY `idx_rel_training_plan_species_species_id` (`species_id`)
) ENGINE=InnoDB COMMENT='Training plan and herb relationship';

-- Structured training content and completion dimensions.
CREATE TABLE `edu_training_plan_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `item_type` VARCHAR(50) NOT NULL COMMENT 'theory/video/experiment/base_practice/project_discussion/report/assessment',
    `item_title` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `course_id` BIGINT NULL,
    `project_id` BIGINT NULL,
    `base_id` BIGINT NULL,
    `species_id` BIGINT NULL,
    `file_id` BIGINT NULL,
    `is_required` TINYINT NOT NULL DEFAULT 1,
    `completion_weight` DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    `sort_order` INT NOT NULL DEFAULT 0,
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
    KEY `idx_training_plan_item_plan_order` (`plan_id`, `sort_order`),
    KEY `idx_training_plan_item_course_id` (`course_id`),
    KEY `idx_training_plan_item_project_id` (`project_id`),
    KEY `idx_training_plan_item_base_id` (`base_id`),
    KEY `idx_training_plan_item_species_id` (`species_id`)
) ENGINE=InnoDB COMMENT='Structured training plan content item';

CREATE TABLE `edu_training_record_evaluation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `training_record_id` BIGINT NOT NULL,
    `dimension_code` VARCHAR(50) NOT NULL COMMENT 'theory/experiment/report/base_practice/project/teacher/final',
    `score` DECIMAL(6,2) NULL,
    `comment` VARCHAR(1000) NULL,
    `evaluator_id` BIGINT NULL,
    `evaluated_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_record_evaluation_record_dimension` (`training_record_id`, `dimension_code`),
    KEY `idx_training_record_evaluation_evaluator_id` (`evaluator_id`)
) ENGINE=InnoDB COMMENT='Training result dimension scores';

-- Generic file relation remains the source of truth for course/project/base/
-- herb files. These indexes make report and cross-module lookups predictable.
CREATE INDEX `idx_sys_file_business_report` ON `sys_file_business` (`biz_type`, `biz_id`, `file_usage`, `sort_order`);
CREATE INDEX `idx_edu_experiment_record_report_file` ON `edu_experiment_record` (`report_file_id`);
CREATE INDEX `idx_edu_training_record_proof_file` ON `edu_training_record` (`completion_proof_file_id`);
