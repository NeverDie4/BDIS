-- Each row in edu_training_record represents one attendance instance.
-- Keep the existing table so historical feedback, reports and evaluations
-- remain attached to the exact attendance that produced them.
ALTER TABLE `edu_training_record`
    ADD COLUMN `attendance_no` VARCHAR(64) NULL COMMENT '一次参加培训的编号';

UPDATE `edu_training_record`
SET `attendance_no` = CONCAT('ATT-', `id`)
WHERE `attendance_no` IS NULL;

CREATE UNIQUE INDEX `uk_edu_training_record_attendance_no`
    ON `edu_training_record` (`attendance_no`);

CREATE INDEX `idx_edu_training_record_plan_user_created`
    ON `edu_training_record` (`plan_id`, `user_id`, `created_at`);
