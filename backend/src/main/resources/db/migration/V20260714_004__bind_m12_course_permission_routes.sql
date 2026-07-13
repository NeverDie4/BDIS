-- M12 课程基础 CRUD 权限路由绑定。
-- 仅更新已存在权限的路由信息，不新增权限，不修改角色权限关系。

UPDATE `auth_permission`
SET `api_path` = '/api/courses',
    `request_method` = 'GET'
WHERE `permission_code` = 'edu:course:list';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}',
    `request_method` = 'GET'
WHERE `permission_code` = 'edu:course:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/courses',
    `request_method` = 'POST'
WHERE `permission_code` = 'edu:course:add';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}',
    `request_method` = 'PUT'
WHERE `permission_code` = 'edu:course:update';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}',
    `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:course:delete';

-- The current permission schema stores one path and one method per permission code.
-- The controller uses edu:course:publish for both publish and offline; the publish
-- route is recorded as the canonical route and both endpoints remain protected by
-- the same permission code in the application layer.
-- The second route is POST /api/courses/{id}/offline and cannot be stored as a
-- second row without duplicating permission_code.
UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/publish',
    `request_method` = 'POST'
WHERE `permission_code` = 'edu:course:publish';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/steps',
    `request_method` = 'GET'
WHERE `permission_code` = 'edu:course-step:list';

-- edu:course-step:save is shared by POST and PUT; POST is the canonical route.
-- The second route is PUT /api/courses/{id}/steps/{stepId} and is protected by
-- the same permission code in the application layer.
UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/steps',
    `request_method` = 'POST'
WHERE `permission_code` = 'edu:course-step:save';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/steps/{stepId}',
    `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:course-step:delete';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/resources',
    `request_method` = 'GET'
WHERE `permission_code` = 'edu:course-resource:list';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/resources',
    `request_method` = 'POST'
WHERE `permission_code` = 'edu:course-resource:add';

UPDATE `auth_permission`
SET `api_path` = '/api/courses/{id}/resources/{resourceId}',
    `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:course-resource:delete';
