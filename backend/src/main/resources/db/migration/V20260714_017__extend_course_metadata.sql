ALTER TABLE `edu_course`
    ADD COLUMN `applicable_majors` TEXT NULL COMMENT '适用专业，JSON 数组',
    ADD COLUMN `hours` INT NULL COMMENT '学时',
    ADD COLUMN `credits` DECIMAL(8,2) NULL COMMENT '学分',
    ADD COLUMN `prerequisites` TEXT NULL COMMENT '先修课程，JSON 数组',
    ADD COLUMN `teaching_objectives` TEXT NULL COMMENT '教学目标，JSON 数组',
    ADD COLUMN `teaching_methods` TEXT NULL COMMENT '教学方式，JSON 数组',
    ADD COLUMN `tags` TEXT NULL COMMENT '课程标签，JSON 数组';
