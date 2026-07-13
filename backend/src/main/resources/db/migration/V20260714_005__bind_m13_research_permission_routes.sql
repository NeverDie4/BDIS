-- M13 课题研究权限路由元数据收口。
-- 仅更新已存在权限的 api_path/request_method，不新增权限、不修改角色关系。
-- 当前 context-path 为 /api，路径格式沿用 V20260711_003 的既有迁移约定。
-- 本文件尚未执行前不得据此宣称数据库路由元数据已生效。

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects', `request_method` = 'GET'
WHERE `permission_code` = 'research:project:list';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'research:project:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects', `request_method` = 'POST'
WHERE `permission_code` = 'research:project:add';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'research:project:update';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/status', `request_method` = 'POST'
WHERE `permission_code` = 'research:project:status';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/members', `request_method` = 'GET'
WHERE `permission_code` = 'research:project-member:list';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/members', `request_method` = 'POST'
WHERE `permission_code` = 'research:project-member:add';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/members/{userId}', `request_method` = 'PUT'
WHERE `permission_code` = 'research:project-member:update';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/members/{userId}', `request_method` = 'DELETE'
WHERE `permission_code` = 'research:project-member:remove';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/materials', `request_method` = 'GET'
WHERE `permission_code` = 'research:project-material:list';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/materials', `request_method` = 'POST'
WHERE `permission_code` = 'research:project-material:add';

UPDATE `auth_permission`
SET `api_path` = '/api/research-projects/{id}/materials/{fileId}', `request_method` = 'DELETE'
WHERE `permission_code` = 'research:project-material:delete';

UPDATE `auth_permission`
SET `api_path` = '/api/research-achievements', `request_method` = 'GET'
WHERE `permission_code` = 'research:achievement:list';

UPDATE `auth_permission`
SET `api_path` = '/api/research-achievements/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'research:achievement:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/research-achievements', `request_method` = 'POST'
WHERE `permission_code` = 'research:achievement:add';

UPDATE `auth_permission`
SET `api_path` = '/api/research-achievements/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'research:achievement:update';
