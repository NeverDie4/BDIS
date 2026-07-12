INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('herb:atlas:import', '批量导入标准图谱', 'api', '从服务端受控目录批量导入标准图谱', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code = 'herb:atlas:import'
WHERE role.role_code = 'ADMIN';
