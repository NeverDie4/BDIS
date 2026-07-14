-- 生物医药数字信息系统 M12-M15 数据库结构补充迁移
-- 适用数据库：MySQL 8.0.x
-- 前置版本：V20260714_009（dev 分支最新迁移之后）
-- 执行方式：通过 Flyway 执行一次
--
-- 存量数据检查已于 2026-07-11 通过：
-- 1. 实验记录 course_id/project_id 同时为空：0
-- 2. 实验记录 course_id/project_id 同时非空：0
-- 3. 培训记录 plan_id 为空：0
-- 4. 重复培训参与记录：无
-- 5. 重复培训反馈：无
--
-- 本迁移执行成功后不得修改，如需调整必须新增更高版本迁移。
ALTER TABLE `edu_course`
    ADD COLUMN `published_at` DATETIME NULL DEFAULT NULL COMMENT 'Course publication time',
    ADD COLUMN `published_by` BIGINT NULL DEFAULT NULL COMMENT 'Publication user ID';

ALTER TABLE `edu_course_resource`
    ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Resource sort order';

ALTER TABLE `rel_project_member`
    ADD COLUMN `member_status` VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT 'Current member status',
    ADD COLUMN `left_at` DATETIME NULL DEFAULT NULL COMMENT 'Most recent leave time';

ALTER TABLE `research_achievement`
    ADD COLUMN `achievement_stage` VARCHAR(50) NULL DEFAULT NULL COMMENT 'Achievement stage',
    ADD COLUMN `achievement_status` VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT 'Achievement status',
    ADD KEY `idx_research_achievement_achievement_status` (`achievement_status`);

ALTER TABLE `edu_experiment_record`
    ADD COLUMN `submitted_at` DATETIME NULL DEFAULT NULL COMMENT 'Submission time',
    ADD COLUMN `submitted_by` BIGINT NULL DEFAULT NULL COMMENT 'Submitting user ID',
    ADD COLUMN `archived_by` BIGINT NULL DEFAULT NULL COMMENT 'Archiving user ID',
    ADD COLUMN `archive_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Archive comment';


ALTER TABLE `edu_experiment_record`
     ADD CONSTRAINT `chk_edu_experiment_record_source`
     CHECK ((`course_id` IS NOT NULL AND `project_id` IS NULL)
         OR (`course_id` IS NULL AND `project_id` IS NOT NULL));

ALTER TABLE `edu_training_plan`
    ADD COLUMN `course_id` BIGINT NULL DEFAULT NULL COMMENT 'Related course ID',
    ADD COLUMN `trainer_id` BIGINT NULL DEFAULT NULL COMMENT 'Training lead user ID',
    ADD COLUMN `location` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Training location',
    ADD COLUMN `published_at` DATETIME NULL DEFAULT NULL COMMENT 'Publication time',
    ADD COLUMN `published_by` BIGINT NULL DEFAULT NULL COMMENT 'Publication user ID',
    ADD KEY `idx_edu_training_plan_course_id` (`course_id`),
    ADD KEY `idx_edu_training_plan_trainer_id` (`trainer_id`);

ALTER TABLE `edu_training_record`
    ADD COLUMN `attendance_status` VARCHAR(50) NULL DEFAULT NULL COMMENT 'Attendance status',
    ADD COLUMN `checked_in_at` DATETIME NULL DEFAULT NULL COMMENT 'Check-in time',
    ADD COLUMN `result_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Training result comment',
    ADD KEY `idx_edu_training_record_attendance_status` (`attendance_status`);

ALTER TABLE `edu_training_record`
    MODIFY COLUMN `plan_id` BIGINT NOT NULL COMMENT 'Training plan ID';

ALTER TABLE `edu_training_record`
    ADD UNIQUE KEY `uk_edu_training_record_plan_user`
        (`plan_id`, `user_id`);
ALTER TABLE `edu_training_feedback`
    ADD UNIQUE KEY `uk_edu_training_feedback_record_user`
        (`training_record_id`, `user_id`);
CREATE TABLE `edu_training_material` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key ID',
    `material_no` VARCHAR(64) NOT NULL COMMENT 'Training material number',
    `material_name` VARCHAR(200) NOT NULL COMMENT 'Training material name',
    `material_type` VARCHAR(50) NOT NULL COMMENT 'Training material type',
    `description` TEXT NULL COMMENT 'Material description',
    `file_id` BIGINT NOT NULL COMMENT 'M05 file resource ID',
    `source_type` VARCHAR(50) NULL DEFAULT 'upload' COMMENT 'Material source type',
    `source_resource_id` BIGINT NULL DEFAULT NULL COMMENT 'Source course resource ID',
    `uploader_id` BIGINT NULL DEFAULT NULL COMMENT 'Uploader user ID',
    `uploaded_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
    `reuse_count` INT NOT NULL DEFAULT 0 COMMENT 'Training-plan reuse count',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1 enabled, 0 disabled',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `created_by` BIGINT NULL DEFAULT NULL COMMENT 'Creator user ID',
    `updated_by` BIGINT NULL DEFAULT NULL COMMENT 'Updater user ID',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT 'Deletion time',
    `deleted_by` BIGINT NULL DEFAULT NULL COMMENT 'Deleter user ID',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Remark',
    `version` INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_edu_training_material_material_no` (`material_no`),
    KEY `idx_edu_training_material_name` (`material_name`),
    KEY `idx_edu_training_material_type` (`material_type`),
    KEY `idx_edu_training_material_file_id` (`file_id`),
    KEY `idx_edu_training_material_uploader_id` (`uploader_id`)
) ENGINE=InnoDB COMMENT='Training material business metadata';


CREATE TABLE `rel_training_plan_material` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key ID',
    `plan_id` BIGINT NOT NULL COMMENT 'Training plan ID',
    `material_id` BIGINT NOT NULL COMMENT 'Training material ID',
    `is_required` TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether the material is required',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Material sort order',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `created_by` BIGINT NULL DEFAULT NULL COMMENT 'Creator user ID',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Remark',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_training_plan_material_plan_material` (`plan_id`, `material_id`),
    KEY `idx_rel_training_plan_material_plan_id` (`plan_id`),
    KEY `idx_rel_training_plan_material_material_id` (`material_id`)
) ENGINE=InnoDB COMMENT='Training-plan and material relation';
