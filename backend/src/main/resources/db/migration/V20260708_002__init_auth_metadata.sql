INSERT IGNORE INTO `auth_role`
(`role_code`, `role_name`, `role_type`, `data_scope`, `description`, `sort_order`, `status`)
VALUES
('ADMIN', '系统管理员', 'system', 'all', '拥有系统管理和权限配置能力', 1, 1),
('TEACHER', '教师', 'business', 'department', '教师账号角色', 10, 1),
('STUDENT', '学生', 'business', 'self', '学生账号角色', 20, 1),
('COLLECTOR', '采集人员', 'business', 'self', '生长数据采集账号角色', 30, 1),
('REVIEWER', '审核人员', 'business', 'department', '业务审核账号角色', 40, 1);

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
VALUES
(0, 'dashboard', '后台首页', '/dashboard', 'dashboard/page', 'dashboard', 1, 1, 1),
(0, 'auth_center', '权限后台', '/dashboard/auth', 'dashboard/auth/page', 'shield', 1, 10, 1);

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT parent.id, 'user_management', '用户管理', '/dashboard/users', 'dashboard/users/page', 'users', 1, 11, 1
FROM `auth_menu` parent
WHERE parent.menu_code = 'auth_center';

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT parent.id, 'role_management', '角色管理', '/dashboard/roles', 'dashboard/roles/page', 'id-card', 1, 12, 1
FROM `auth_menu` parent
WHERE parent.menu_code = 'auth_center';

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT parent.id, 'permission_management', '菜单权限', '/dashboard/permissions', 'dashboard/permissions/page', 'key', 1, 13, 1
FROM `auth_menu` parent
WHERE parent.menu_code = 'auth_center';

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT parent.id, 'organization_management', '组织机构', '/dashboard/organizations', 'dashboard/organizations/page', 'building', 1, 14, 1
FROM `auth_menu` parent
WHERE parent.menu_code = 'auth_center';

INSERT IGNORE INTO `auth_menu`
(`parent_id`, `menu_code`, `menu_name`, `route_path`, `component_path`, `icon`, `visible`, `sort_order`, `status`)
SELECT parent.id, 'department_management', '部门管理', '/dashboard/departments', 'dashboard/departments/page', 'sitemap', 1, 15, 1
FROM `auth_menu` parent
WHERE parent.menu_code = 'auth_center';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:dashboard:view', '查看后台首页', 'menu', id, '后台首页菜单权限', 1
FROM `auth_menu`
WHERE menu_code = 'dashboard';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:center:view', '查看权限后台', 'menu', id, '权限后台菜单权限', 1
FROM `auth_menu`
WHERE menu_code = 'auth_center';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:user:view', '查看用户', 'menu', id, '用户管理菜单与查询权限', 1
FROM `auth_menu`
WHERE menu_code = 'user_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:role:view', '查看角色', 'menu', id, '角色管理菜单与查询权限', 1
FROM `auth_menu`
WHERE menu_code = 'role_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:permission:view', '查看权限', 'menu', id, '菜单权限管理菜单与查询权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:organization:view', '查看组织', 'menu', id, '组织机构菜单与查询权限', 1
FROM `auth_menu`
WHERE menu_code = 'organization_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:department:view', '查看部门', 'menu', id, '部门管理菜单与查询权限', 1
FROM `auth_menu`
WHERE menu_code = 'department_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:user:create', '新增用户', 'button', id, '新增用户按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'user_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:user:update', '编辑用户', 'button', id, '编辑用户按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'user_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:user:delete', '删除用户', 'button', id, '删除用户按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'user_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:user:assign-role', '分配角色', 'button', id, '用户分配角色按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'user_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:role:create', '新增角色', 'button', id, '新增角色按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'role_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:role:update', '编辑角色', 'button', id, '编辑角色按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'role_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:role:delete', '删除角色', 'button', id, '删除角色按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'role_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:role:assign-permission', '分配权限', 'button', id, '角色分配权限按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'role_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:permission:create', '新增权限', 'button', id, '新增权限按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:permission:update', '编辑权限', 'button', id, '编辑权限按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:permission:delete', '删除权限', 'button', id, '删除权限按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:menu:view', '查看菜单', 'button', id, '菜单查询按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:menu:create', '新增菜单', 'button', id, '新增菜单按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:menu:update', '编辑菜单', 'button', id, '编辑菜单按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:menu:delete', '删除菜单', 'button', id, '删除菜单按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'permission_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:organization:create', '新增组织', 'button', id, '新增组织按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'organization_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:organization:update', '编辑组织', 'button', id, '编辑组织按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'organization_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:organization:delete', '删除组织', 'button', id, '删除组织按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'organization_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:department:create', '新增部门', 'button', id, '新增部门按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'department_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:department:update', '编辑部门', 'button', id, '编辑部门按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'department_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `menu_id`, `description`, `status`)
SELECT 'auth:department:delete', '删除部门', 'button', id, '删除部门按钮权限', 1
FROM `auth_menu`
WHERE menu_code = 'department_management';

INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('auth:authorization:decide', '权限判定', 'api', '权限判定接口权限', 1),
('auth:data-scope:view', '查看数据范围', 'api', '查看角色数据范围权限', 1),
('auth:data-scope:update', '维护数据范围', 'api', '维护角色数据范围权限', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
CROSS JOIN `auth_permission` permission
WHERE role.role_code = 'ADMIN';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission ON permission.permission_code = 'auth:dashboard:view'
WHERE role.role_code IN ('TEACHER', 'STUDENT', 'COLLECTOR', 'REVIEWER');
