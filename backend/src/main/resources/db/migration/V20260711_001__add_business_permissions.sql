INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('dashboard:view', '查看业务看板', 'api', '查看业务汇总、待办和地图概览', 1),
('herb:species:view', '查看药材档案', 'api', '查询药材档案和基础信息', 1),
('herb:species:create', '新增药材档案', 'api', '新增药材档案', 1),
('herb:species:update', '编辑药材档案', 'api', '编辑药材档案', 1),
('herb:species:delete', '删除药材档案', 'api', '删除药材档案', 1),
('map:point:view', '查看地图点位', 'api', '查询地图点位和点位采集记录', 1),
('map:point:create', '新增地图点位', 'api', '新增地图点位', 1),
('map:point:update', '编辑地图点位', 'api', '编辑地图点位', 1),
('map:point:delete', '删除地图点位', 'api', '删除地图点位', 1),
('growth:record:view', '查看生长记录', 'api', '查询生长采集、任务和批次', 1),
('growth:record:create', '新增生长记录', 'api', '新增采集任务、批次和生长记录', 1),
('growth:record:update', '编辑生长记录', 'api', '编辑采集任务、批次和生长记录', 1),
('growth:record:delete', '删除生长记录', 'api', '删除采集任务、批次和生长记录', 1),
('growth:record:submit', '提交生长记录', 'api', '发布任务、提交采集记录和批次', 1),
('growth:record:audit', '审核生长记录', 'api', '确认、退回、归档和重新复核采集数据', 1),
('file:resource:view', '查看文件资源', 'api', '查询、预览和下载授权文件', 1),
('file:resource:upload', '上传文件资源', 'api', '上传文件并关联业务对象', 1),
('file:resource:update', '维护文件关联', 'api', '绑定和解除业务文件关联', 1),
('file:resource:delete', '删除文件资源', 'api', '删除文件资源', 1),
('audit:log:view', '查看审计日志', 'api', '查询登录、操作、文件访问和同步日志', 1),
('herb:identification:view', '查看图谱识别', 'api', '查询图谱、特征、匹配和识别结果', 1),
('herb:identification:execute', '执行图谱识别', 'api', '上传图谱、提取特征并执行识别', 1),
('herb:identification:review', '复核图谱识别', 'api', '人工复核图谱识别结论', 1),
('soap:exchange:view', '查看 SOAP 交换', 'api', '查询 SOAP 任务、记录和字段映射', 1),
('soap:exchange:execute', '执行 SOAP 交换', 'api', '创建和重试 SOAP 交换任务', 1),
('dictionary:view', '查看数据字典', 'api', '查询字典类型、字典项和区域树', 1),
('dictionary:manage', '维护数据字典', 'api', '新增、编辑和删除字典数据', 1),
('map:base:view', '查看药材基地', 'api', '查询药材基地', 1),
('map:base:manage', '维护药材基地', 'api', '新增、编辑和删除药材基地', 1),
('course:view', '查看实验课程', 'api', '查询实验课程、步骤和资源', 1),
('course:manage', '维护实验课程', 'api', '新增、编辑课程、步骤和资源', 1),
('course:publish', '发布实验课程', 'api', '发布和下架实验课程', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'dashboard:view',
    'herb:species:view',
    'map:point:view',
    'growth:record:view',
    'file:resource:view',
    'herb:identification:view',
    'dictionary:view',
    'map:base:view',
    'course:view'
  )
WHERE role.role_code IN ('TEACHER', 'STUDENT', 'COLLECTOR', 'REVIEWER');

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'herb:species:create',
    'herb:species:update',
    'map:point:create',
    'map:point:update',
    'growth:record:create',
    'growth:record:update',
    'growth:record:submit',
    'file:resource:upload',
    'file:resource:update',
    'course:manage',
    'course:publish'
  )
WHERE role.role_code = 'TEACHER';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'growth:record:create',
    'growth:record:update',
    'growth:record:submit',
    'file:resource:upload',
    'herb:identification:execute'
  )
WHERE role.role_code = 'COLLECTOR';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'growth:record:audit',
    'herb:identification:review',
    'audit:log:view'
  )
WHERE role.role_code = 'REVIEWER';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
CROSS JOIN `auth_permission` permission
WHERE role.role_code = 'ADMIN';
