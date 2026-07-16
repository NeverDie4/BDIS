INSERT IGNORE INTO `auth_role`
(`role_code`, `role_name`, `role_type`, `data_scope`, `description`, `sort_order`, `status`)
VALUES
('AUDITOR', '系统审计员', 'system', 'all', '查询系统操作、登录、文件访问和数据同步日志', 5, 1);

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
VALUES
(0, 'audit_center', '操作审计', '/dashboard/audit', 'dashboard/audit/page', 'clipboard-list', 1, 20, 1);

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'audit:center:view', '查看操作审计中心', 'menu', id, '进入操作审计后台页面', 1
FROM `auth_menu`
WHERE `menu_code` = 'audit_center';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('audit:operation:view', '查看操作日志', 'api', '查询系统操作日志', 1),
('audit:login:view', '查看登录日志', 'api', '查询账号登录日志', 1),
('audit:file:view', '查看文件访问日志', 'api', '查询文件预览和下载日志', 1),
('audit:data-sync:view', '查看数据同步日志', 'api', '查询数据同步和 SOAP 交换日志', 1);

DELETE relation
FROM `rel_role_permission` relation
JOIN `auth_role` role ON role.id = relation.role_id
JOIN `auth_permission` permission ON permission.id = relation.permission_id
WHERE role.role_code = 'REVIEWER'
  AND permission.permission_code = 'audit:log:view';

UPDATE `auth_permission`
SET `status` = 0,
    `description` = '已由细分审计权限替代'
WHERE `permission_code` = 'audit:log:view';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'audit:center:view',
    'audit:operation:view',
    'audit:login:view',
    'audit:file:view',
    'audit:data-sync:view'
  )
WHERE role.role_code IN ('ADMIN', 'AUDITOR');
