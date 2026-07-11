INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('dashboard:refresh', '刷新看板快照', 'api', '重新计算并保存当日看板快照', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code = 'dashboard:refresh'
WHERE role.role_code = 'ADMIN';
