-- Student course enrollment and learning-progress persistence.

CREATE TABLE `edu_course_enrollment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `enrollment_status` VARCHAR(50) NOT NULL DEFAULT 'enrolled' COMMENT 'enrolled/completed/dropped',
    `progress` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    `score` DECIMAL(5,2) NULL,
    `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `completed_at` DATETIME NULL,
    `last_accessed_at` DATETIME NULL,
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
    UNIQUE KEY `uk_course_enrollment_course_user` (`course_id`, `user_id`),
    KEY `idx_course_enrollment_user_status` (`user_id`, `enrollment_status`, `status`, `is_deleted`),
    KEY `idx_course_enrollment_course_status` (`course_id`, `enrollment_status`, `status`, `is_deleted`)
) ENGINE=InnoDB COMMENT='Student course enrollment and aggregate progress';

CREATE TABLE `edu_course_learning_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `enrollment_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `item_type` VARCHAR(50) NOT NULL COMMENT 'resource/video/step',
    `item_id` BIGINT NOT NULL,
    `progress_value` DECIMAL(7,2) NOT NULL DEFAULT 0.00 COMMENT 'Percentage or item-specific progress',
    `progress_seconds` INT NULL,
    `total_seconds` INT NULL,
    `completed` TINYINT NOT NULL DEFAULT 0,
    `first_accessed_at` DATETIME NULL,
    `last_accessed_at` DATETIME NULL,
    `completed_at` DATETIME NULL,
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
    UNIQUE KEY `uk_course_learning_progress_enrollment_item` (`enrollment_id`, `item_type`, `item_id`),
    KEY `idx_course_learning_progress_user_course` (`user_id`, `course_id`, `item_type`),
    KEY `idx_course_learning_progress_course_item` (`course_id`, `item_type`, `item_id`)
) ENGINE=InnoDB COMMENT='Student course item learning progress';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `api_path`, `request_method`, `description`, `status`)
VALUES
('edu:course:enroll', 'edu:course:enroll', 'api', '/api/courses/{id}/enrollment', 'POST', 'Enroll in published course', 1),
('edu:course:enrollment:list', 'edu:course:enrollment:list', 'api', '/api/courses/enrollments', 'GET', 'View course enrollments', 1),
('edu:course-learning:list', 'edu:course-learning:list', 'api', '/api/courses/{id}/learning', 'GET', 'View course learning progress', 1),
('edu:course-learning:save', 'edu:course-learning:save', 'api', '/api/courses/{id}/learning', 'PUT', 'Save course learning progress', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'STUDENT'
  AND p.permission_code IN (
    'edu:course:enroll', 'edu:course-learning:list', 'edu:course-learning:save'
  );

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER', 'REVIEWER')
  AND p.permission_code IN ('edu:course:enrollment:list', 'edu:course-learning:list');
