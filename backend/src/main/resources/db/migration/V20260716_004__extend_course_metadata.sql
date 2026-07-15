-- This migration may be reached after the old 20260714.017 migration has
-- already added these columns. Keep each operation idempotent so Flyway can
-- advance an existing development database as well as initialize a new one.

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'applicable_majors'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `applicable_majors` TEXT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'hours'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `hours` INT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'credits'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `credits` DECIMAL(8,2) NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'prerequisites'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `prerequisites` TEXT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'teaching_objectives'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `teaching_objectives` TEXT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'teaching_methods'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `teaching_methods` TEXT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'tags'), 'SELECT 1', 'ALTER TABLE `edu_course` ADD COLUMN `tags` TEXT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
