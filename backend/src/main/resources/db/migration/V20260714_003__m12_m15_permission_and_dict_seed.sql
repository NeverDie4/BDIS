-- 生物医药数字信息系统 M12-M15 权限与数据字典初始化
-- 适用数据库：MySQL 8.0.x
-- 前置版本：V20260713_004
-- 执行方式：通过 Flyway 执行一次
--
-- 本迁移包含：
-- 1. M12-M15 接口权限编码；
-- 2. M12-M15 业务字典类型；
-- 3. M12-M15 业务字典项。
--
-- 本迁移不包含：
-- 1. 演示业务数据；
-- 2. 用户、角色或角色权限关系；
-- 3. 菜单数据；
-- 4. Controller 接口路径绑定。
--
-- Controller 尚未实现，因此 auth_permission.api_path 和
-- auth_permission.request_method 暂不写入。
-- Controller 路由确定后，必须通过新的更高版本 Flyway 迁移补充，
-- 不得修改已经执行的本迁移。
--
-- 本迁移执行成功后不得修改，如需调整必须新增更高版本迁移。

INSERT IGNORE INTO `auth_permission` (`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`) VALUES
('edu:course:list', 'edu:course:list', 'api', NULL, 'M12 permission', 1),
('edu:course:detail', 'edu:course:detail', 'api', NULL, 'M12 permission', 1),
('edu:course:add', 'edu:course:add', 'api', NULL, 'M12 permission', 1),
('edu:course:update', 'edu:course:update', 'api', NULL, 'M12 permission', 1),
('edu:course:delete', 'edu:course:delete', 'api', NULL, 'M12 permission', 1),
('edu:course:publish', 'edu:course:publish', 'api', NULL, 'M12 permission', 1),
('edu:course-step:list', 'edu:course-step:list', 'api', NULL, 'M12 permission', 1),
('edu:course-step:save', 'edu:course-step:save', 'api', NULL, 'M12 permission', 1),
('edu:course-step:delete', 'edu:course-step:delete', 'api', NULL, 'M12 permission', 1),
('edu:course-resource:list', 'edu:course-resource:list', 'api', NULL, 'M12 permission', 1),
('edu:course-resource:add', 'edu:course-resource:add', 'api', NULL, 'M12 permission', 1),
('edu:course-resource:delete', 'edu:course-resource:delete', 'api', NULL, 'M12 permission', 1),
('research:project:list', 'research:project:list', 'api', NULL, 'M13 permission', 1),
('research:project:detail', 'research:project:detail', 'api', NULL, 'M13 permission', 1),
('research:project:add', 'research:project:add', 'api', NULL, 'M13 permission', 1),
('research:project:update', 'research:project:update', 'api', NULL, 'M13 permission', 1),
('research:project:status', 'research:project:status', 'api', NULL, 'M13 permission', 1),
('research:project-member:list', 'research:project-member:list', 'api', NULL, 'M13 permission', 1),
('research:project-member:add', 'research:project-member:add', 'api', NULL, 'M13 permission', 1),
('research:project-member:update', 'research:project-member:update', 'api', NULL, 'M13 permission', 1),
('research:project-member:remove', 'research:project-member:remove', 'api', NULL, 'M13 permission', 1),
('research:project-material:list', 'research:project-material:list', 'api', NULL, 'M13 permission', 1),
('research:project-material:add', 'research:project-material:add', 'api', NULL, 'M13 permission', 1),
('research:project-material:delete', 'research:project-material:delete', 'api', NULL, 'M13 permission', 1),
('research:achievement:list', 'research:achievement:list', 'api', NULL, 'M13 permission', 1),
('research:achievement:detail', 'research:achievement:detail', 'api', NULL, 'M13 permission', 1),
('research:achievement:add', 'research:achievement:add', 'api', NULL, 'M13 permission', 1),
('research:achievement:update', 'research:achievement:update', 'api', NULL, 'M13 permission', 1),
('edu:experiment-record:list', 'edu:experiment-record:list', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:detail', 'edu:experiment-record:detail', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:add', 'edu:experiment-record:add', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:update', 'edu:experiment-record:update', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:delete', 'edu:experiment-record:delete', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:submit', 'edu:experiment-record:submit', 'api', NULL, 'M14 permission', 1),
('edu:experiment-record:archive', 'edu:experiment-record:archive', 'api', NULL, 'M14 permission', 1),
('edu:experiment-attachment:list', 'edu:experiment-attachment:list', 'api', NULL, 'M14 permission', 1),
('edu:experiment-attachment:add', 'edu:experiment-attachment:add', 'api', NULL, 'M14 permission', 1),
('edu:experiment-attachment:delete', 'edu:experiment-attachment:delete', 'api', NULL, 'M14 permission', 1),
('edu:training-plan:list', 'edu:training-plan:list', 'api', NULL, 'M15 permission', 1),
('edu:training-plan:detail', 'edu:training-plan:detail', 'api', NULL, 'M15 permission', 1),
('edu:training-plan:add', 'edu:training-plan:add', 'api', NULL, 'M15 permission', 1),
('edu:training-plan:update', 'edu:training-plan:update', 'api', NULL, 'M15 permission', 1),
('edu:training-plan:delete', 'edu:training-plan:delete', 'api', NULL, 'M15 permission', 1),
('edu:training-plan:publish', 'edu:training-plan:publish', 'api', NULL, 'M15 permission', 1),
('edu:training-material:list', 'edu:training-material:list', 'api', NULL, 'M15 permission', 1),
('edu:training-material:detail', 'edu:training-material:detail', 'api', NULL, 'M15 permission', 1),
('edu:training-material:add', 'edu:training-material:add', 'api', NULL, 'M15 permission', 1),
('edu:training-material:update', 'edu:training-material:update', 'api', NULL, 'M15 permission', 1),
('edu:training-material:delete', 'edu:training-material:delete', 'api', NULL, 'M15 permission', 1),
('edu:training-material:bind', 'edu:training-material:bind', 'api', NULL, 'M15 permission', 1),
('edu:training-record:list', 'edu:training-record:list', 'api', NULL, 'M15 permission', 1),
('edu:training-record:detail', 'edu:training-record:detail', 'api', NULL, 'M15 permission', 1),
('edu:training-record:add', 'edu:training-record:add', 'api', NULL, 'M15 permission', 1),
('edu:training-record:update', 'edu:training-record:update', 'api', NULL, 'M15 permission', 1),
('edu:training-feedback:list', 'edu:training-feedback:list', 'api', NULL, 'M15 permission', 1),
('edu:training-feedback:add', 'edu:training-feedback:add', 'api', NULL, 'M15 permission', 1),
('edu:training-feedback:update', 'edu:training-feedback:update', 'api', NULL, 'M15 permission', 1),
('edu:training-summary:view', 'edu:training-summary:view', 'api', NULL, 'M15 permission', 1);

INSERT IGNORE INTO `dict_type` (`type_code`, `type_name`, `sort_order`, `status`) VALUES
('course_type', 'course_type', 0, 1),
('course_publish_status', 'course_publish_status', 0, 1),
('project_type', 'project_type', 0, 1),
('project_status', 'project_status', 0, 1),
('project_member_role', 'project_member_role', 0, 1),
('project_member_status', 'project_member_status', 0, 1),
('achievement_type', 'achievement_type', 0, 1),
('achievement_stage', 'achievement_stage', 0, 1),
('achievement_status', 'achievement_status', 0, 1),
('experiment_archive_status', 'experiment_archive_status', 0, 1),
('training_plan_type', 'training_plan_type', 0, 1),
('training_publish_status', 'training_publish_status', 0, 1),
('training_material_type', 'training_material_type', 0, 1),
('training_status', 'training_status', 0, 1),
('attendance_status', 'attendance_status', 0, 1);

INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'experiment', '实验', 'experiment', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'course_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'online', '在线', 'online', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'course_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'mixed', '混合', 'mixed', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'course_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'draft', '草稿', 'draft', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'course_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'published', '已发布', 'published', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'course_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'offline', '已下线', 'offline', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'course_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'teaching', '教学', 'teaching', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'project_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'research', '研究', 'research', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'project_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'cooperation', '合作', 'cooperation', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'project_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'planning', '规划中', 'planning', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'project_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'ongoing', '进行中', 'ongoing', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'project_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'suspended', '已暂停', 'suspended', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'project_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'completed', '已完成', 'completed', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'project_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'leader', '负责人', 'leader', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_role';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'researcher', '研究员', 'researcher', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_role';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'assistant', '助理', 'assistant', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_role';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'student', '学生', 'student', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_role';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'active', '在岗', 'active', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'left', '已离岗', 'left', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'project_member_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'paper', '论文', 'paper', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'patent', '专利', 'patent', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'report', '报告', 'report', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'experiment_result', '实验结果', 'experiment_result', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'initial', '初期', 'initial', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_stage';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'middle', '中期', 'middle', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_stage';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'final', '末期', 'final', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_stage';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'draft', '草稿', 'draft', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'submitted', '已提交', 'submitted', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'confirmed', '已确认', 'confirmed', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'achievement_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'draft', '草稿', 'draft', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'experiment_archive_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'submitted', '已提交', 'submitted', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'experiment_archive_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'archived', '已归档', 'archived', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'experiment_archive_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'course', '课程', 'course', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'training_plan_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'seminar', '研讨会', 'seminar', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'training_plan_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'practice', '实践', 'practice', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'training_plan_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'online', '在线', 'online', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'training_plan_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'draft', '草稿', 'draft', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'training_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'published', '已发布', 'published', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'training_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'closed', '已关闭', 'closed', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'training_publish_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'courseware', '课件', 'courseware', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'training_material_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'handout', '讲义', 'handout', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'training_material_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'video', '视频', 'video', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'training_material_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'case', '案例', 'case', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'training_material_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'exercise', '练习', 'exercise', 0, 50, 1
FROM `dict_type`
WHERE `type_code` = 'training_material_type';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'not_started', '未开始', 'not_started', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'training_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'learning', '学习中', 'learning', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'training_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'completed', '已完成', 'completed', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'training_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'failed', '失败', 'failed', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'training_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'makeup', '补考', 'makeup', 0, 50, 1
FROM `dict_type`
WHERE `type_code` = 'training_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'pending', '待处理', 'pending', 0, 10, 1
FROM `dict_type`
WHERE `type_code` = 'attendance_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'present', '出勤', 'present', 0, 20, 1
FROM `dict_type`
WHERE `type_code` = 'attendance_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'late', '迟到', 'late', 0, 30, 1
FROM `dict_type`
WHERE `type_code` = 'attendance_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'absent', '缺勤', 'absent', 0, 40, 1
FROM `dict_type`
WHERE `type_code` = 'attendance_status';
INSERT IGNORE INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`,
 `parent_id`, `sort_order`, `status`)
SELECT `id`, 'leave', '请假', 'leave', 0, 50, 1
FROM `dict_type`
WHERE `type_code` = 'attendance_status';
