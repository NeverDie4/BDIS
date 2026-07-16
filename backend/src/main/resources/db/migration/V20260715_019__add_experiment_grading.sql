-- Persist teacher grading for submitted experiment records.
ALTER TABLE `edu_experiment_record`
    ADD COLUMN `score` DECIMAL(5,2) NULL DEFAULT NULL COMMENT 'Experiment score from 0 to 100',
    ADD COLUMN `graded_by` BIGINT NULL DEFAULT NULL COMMENT 'Grading user ID',
    ADD COLUMN `graded_at` DATETIME NULL DEFAULT NULL COMMENT 'Grading time',
    ADD COLUMN `grade_comment` VARCHAR(1000) NULL DEFAULT NULL COMMENT 'Grading comment';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `api_path`, `request_method`, `description`, `status`)
VALUES
('edu:experiment-record:grade', 'edu:experiment-record:grade', 'api', '/api/experiment-records/{id}/grade', 'POST', 'M14 teacher grading permission', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER', 'REVIEWER')
  AND p.permission_code = 'edu:experiment-record:grade';
