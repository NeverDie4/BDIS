-- M12-M15 role authorization migration.
-- This migration is intentionally additive and idempotent; do not edit prior migrations.

INSERT IGNORE INTO `auth_role`
(`role_code`, `role_name`, `role_type`, `data_scope`, `description`, `sort_order`, `status`)
VALUES
('RESEARCHER', '研究人员', 'business', 'department', 'M13科研项目与M14实验记录业务角色', 50, 1),
('TRAINER', '培训管理员', 'business', 'department', 'M15培训计划与培训资源管理角色', 60, 1);

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT 0, 'teaching', '教学科研', '/teaching', 'teaching/page', 'book-open', 1, 20, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `auth_menu` WHERE `menu_code` = 'teaching'
);

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'edu:teaching:view', '查看教学科研页面', 'menu', id, 'M12-M15 教学科研页面访问权限', 1
FROM `auth_menu`
WHERE `menu_code` = 'teaching';

-- ADMIN already receives all permissions through the baseline authorization seed.
-- TEACHER: full M12 and full M15 management permissions.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'TEACHER'
  AND (
    p.permission_code LIKE 'edu:course:%'
    OR p.permission_code LIKE 'edu:course-step:%'
    OR p.permission_code LIKE 'edu:course-resource:%'
    OR p.permission_code LIKE 'edu:training-%'
  );

-- STUDENT: browse published teaching content, manage own experiment/training records,
-- and submit/update feedback. Data-scope enforcement remains in the services.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'STUDENT'
  AND p.permission_code IN (
    'edu:teaching:view',
    'edu:course:list', 'edu:course:detail',
    'edu:course-step:list', 'edu:course-resource:list',
    'edu:experiment-record:list', 'edu:experiment-record:detail',
    'edu:experiment-record:add', 'edu:experiment-record:update',
    'edu:experiment-record:delete', 'edu:experiment-record:submit',
    'edu:experiment-attachment:list', 'edu:experiment-attachment:add',
    'edu:experiment-attachment:delete',
    'edu:training-plan:list', 'edu:training-plan:detail',
    'edu:training-material:list', 'edu:training-material:detail',
    'edu:training-record:list', 'edu:training-record:detail',
    'edu:training-record:update',
    'edu:training-feedback:list', 'edu:training-feedback:add',
    'edu:training-feedback:update'
  );

-- RESEARCHER: full M13 project/achievement operations and M14 experiment operations,
-- excluding archive/review actions reserved for REVIEWER.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'RESEARCHER'
  AND p.permission_code IN (
    'edu:teaching:view',
    'research:project:list', 'research:project:detail',
    'research:project:add', 'research:project:update',
    'research:project:status',
    'research:project-member:list', 'research:project-member:add',
    'research:project-member:update', 'research:project-member:remove',
    'research:project-material:list', 'research:project-material:add',
    'research:project-material:delete',
    'research:achievement:list', 'research:achievement:detail',
    'research:achievement:add', 'research:achievement:update',
    'edu:experiment-record:list', 'edu:experiment-record:detail',
    'edu:experiment-record:add', 'edu:experiment-record:update',
    'edu:experiment-record:delete', 'edu:experiment-record:submit',
    'edu:experiment-attachment:list', 'edu:experiment-attachment:add',
    'edu:experiment-attachment:delete'
  );

-- TRAINER: full M15 operations.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'TRAINER'
  AND (
    p.permission_code = 'edu:teaching:view'
    OR p.permission_code LIKE 'edu:training-%'
  );

-- REVIEWER: read/review access across M13-M15 without authoring permissions.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
JOIN `auth_permission` p
WHERE r.role_code = 'REVIEWER'
  AND p.permission_code IN (
    'edu:teaching:view',
    'research:project:list', 'research:project:detail',
    'edu:experiment-record:list', 'edu:experiment-record:detail',
    'edu:experiment-record:archive',
    'edu:training-plan:list', 'edu:training-plan:detail',
    'edu:training-material:list', 'edu:training-material:detail',
    'edu:training-record:list', 'edu:training-record:detail',
    'edu:training-feedback:list', 'edu:training-summary:view'
  );

-- COLLECTOR intentionally receives no M12-M15 permission.
