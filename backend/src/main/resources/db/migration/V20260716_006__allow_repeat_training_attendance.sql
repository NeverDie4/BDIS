-- Each training record is one attendance instance. Remove the legacy uniqueness
-- that limited a learner to one attendance per plan.
SELECT IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'edu_training_record'
          AND index_name = 'uk_edu_training_record_plan_user'
    ),
    'ALTER TABLE `edu_training_record` DROP INDEX `uk_edu_training_record_plan_user`',
    'SELECT 1'
) INTO @drop_training_plan_user_index;

PREPARE stmt FROM @drop_training_plan_user_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
